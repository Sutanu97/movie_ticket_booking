package com.projects.MovieTicketBookingSystem.controller;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dao.UserRepository;
import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.dto.RazorPayOrderDto;
import com.projects.MovieTicketBookingSystem.dto.RefundEvent;
import com.projects.MovieTicketBookingSystem.dto.TicketBookingConfig;
import com.projects.MovieTicketBookingSystem.entity.RazorPayOrderStatus;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import com.projects.MovieTicketBookingSystem.util.NotificationUtil;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.mail.MessagingException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@RestController
public class ShowTicketBookingController {

    private TicketBookingManager ticketBookingManager;
    private UserRepository userRepository;
    private RedissonClient redissonClient;
    private NotificationUtil notificationUtil;
    private KafkaTemplate<String, RefundEvent> kafkaTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTicketBookingController.class);
    private static final String REFUND_TOPIC = "booking.refund";

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    public ShowTicketBookingController(TicketBookingManager ticketBookingManager, UserRepository userRepository,
            RedissonClient redissonClient, NotificationUtil notificationUtil,
            KafkaTemplate<String, RefundEvent> kafkaTemplate) {
        this.ticketBookingManager = ticketBookingManager;
        this.userRepository = userRepository;
        this.redissonClient = redissonClient;
        this.notificationUtil = notificationUtil;
        this.kafkaTemplate = kafkaTemplate;
    }


    @GetMapping("/shows/user/{username}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #username = authentication.principal.username)")
    public List<BookingSummary> getBookingsForUser(@PathVariable String username){
        LOGGER.debug("In method getBookingsForUser");
        return ticketBookingManager.getBookingsForUser(username);
    }


    @PostMapping("/shows/book")
    public ResponseEntity<Object> initiateTicketBooking(@RequestBody TicketBookingConfig ticketBookingConfig){
        LOGGER.debug("In method bookTickets");
        Integer showId = ticketBookingConfig.getShow().getPkMovieShowId();
        Set<String> seats = Arrays.stream(ticketBookingConfig.getSeats().split(" "))
                .collect(Collectors.toSet());
        Map<String, String> bookingStatus = ticketBookingManager.bookShow(ticketBookingConfig.getShow(), seats);
        if(bookingStatus != null
            &&
                bookingStatus.containsKey(TicketBookingConstants.SEATS_RESERVATION_STATUS)
            &&
                bookingStatus.get(TicketBookingConstants.SEATS_RESERVATION_STATUS).equals(TicketBookingConstants.SUCCESS)){
            if(bookingStatus.containsKey(TicketBookingConstants.RAZORPAY_ORDER)
                    &&
                    bookingStatus.get(TicketBookingConstants.RAZORPAY_ORDER) != null){
                String msg = "Seats have been reserved and payment has been initiated. Booking will be confirmed after payment is processed!";
                LOGGER.info(msg);
                RazorPayOrderDto razorPayOrderDto = new RazorPayOrderDto(TicketBookingConstants.SUCCESS, bookingStatus.get(TicketBookingConstants.RAZORPAY_ORDER),
                        Double.parseDouble(bookingStatus.get(TicketBookingConstants.RAZORPAY_AMOUNT)));
                return ResponseEntity.status(HttpStatusCode.valueOf(200))
                        .body(razorPayOrderDto);
            }else{
                String msg = bookingStatus.get(TicketBookingConstants.ERROR_MESSAGE);
                return ResponseEntity.status(HttpStatusCode.valueOf(500))
                        .body(msg);
            }
        }else {
            String msg = bookingStatus.get(TicketBookingConstants.ERROR_MESSAGE);
            return ResponseEntity.status(HttpStatusCode.valueOf(500))
                    .body(msg);
        }
    }

    @PostMapping("/booking_confirmation")
    public ResponseEntity<String> confirmBooking(@RequestBody TicketBookingConfig ticketBookingConfig) throws IOException, MessagingException {
        LOGGER.debug("In method confirmBooking");
        String orderId        = ticketBookingConfig.getOrderId();
        String paymentId      = ticketBookingConfig.getRazorpayPaymentId();
        String signature      = ticketBookingConfig.getRazorpaySignature();
        Show show             = ticketBookingConfig.getShow();
        Set<String> seatsBooked = Arrays.stream(ticketBookingConfig.getSeats().split(" ")).collect(Collectors.toSet());

        // Step 1: Server-side signature verification — prevents tampered/replayed payment callbacks.
        // The frontend CANNOT fake this: only Razorpay + our secret key can produce a valid HMAC.
        if (paymentId != null && signature != null) {
            try {
                JSONObject signaturePayload = new JSONObject();
                signaturePayload.put("razorpay_order_id", orderId);
                signaturePayload.put("razorpay_payment_id", paymentId);
                signaturePayload.put("razorpay_signature", signature);
                boolean isValidSignature = Utils.verifyPaymentSignature(signaturePayload, razorpaySecret);
                if (!isValidSignature) {
                    LOGGER.warn("Invalid Razorpay signature received for orderId: {}", orderId);
                    return ResponseEntity.status(400).body("Payment verification failed: invalid signature.");
                }
                LOGGER.info("Razorpay signature verified successfully for orderId: {}", orderId);
            } catch (RazorpayException e) {
                LOGGER.error("Signature verification error for orderId: {}", orderId, e);
                return ResponseEntity.status(400).body("Payment verification failed.");
            }
        } else {
            // Payment ID or signature absent — reject the request outright
            LOGGER.warn("Missing paymentId or signature in booking confirmation for orderId: {}", orderId);
            return ResponseEntity.status(400).body("Missing payment details. Cannot confirm booking.");
        }

        // Step 2: Check order status and confirm booking
        if (ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())) {
            LOGGER.info("Trying to acquire the lock for orderId: {}", orderId);
            RLock lock = redissonClient.getLock("lock:order:" + orderId);
            try {
                lock.lock();
                LOGGER.info("Lock has been acquired for orderId: {}", orderId);
                // Double-check inside lock to guard against concurrent timeout + confirmation race
                if (ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())) {
                    LOGGER.info("Going to update the Razorpay order with status {}", TicketBookingConstants.SUCCESS);
                    ticketBookingManager.updateRazorpayOrder(orderId, TicketBookingConstants.SUCCESS);
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    String emailId = this.userRepository.findByUsername(username).get().getEmailId();
                    notificationUtil.sendConfirmationEmail(orderId, show, emailId, seatsBooked);
                    this.ticketBookingManager.addToHistory(show, orderId, seatsBooked);
                    return ResponseEntity.ok().body("Booking has been confirmed!");
                } else {
                    // Race: timeout fired between outer check and acquiring lock.
                    // Signature is valid — user actually paid. Trigger async refund via Kafka.
                    LOGGER.warn("Order {} expired between status check and lock acquisition. Payment was captured. Publishing refund event.", orderId);
                    RefundEvent refundEvent = new RefundEvent(orderId, paymentId, seatsBooked.size());
                    kafkaTemplate.send(REFUND_TOPIC, orderId, refundEvent);
                    return ResponseEntity.ok().body("Your session expired but payment was received. A refund will be processed shortly.");
                }
            } finally {
                lock.unlock();
            }
        } else {
            // Order is not in Created state — either already confirmed or timed out before this call.
            // Since we passed signature verification above, the payment IS real — initiate refund.
            LOGGER.warn("Order {} is not in Created state. Payment was captured after expiry. Publishing refund event.", orderId);
            RefundEvent refundEvent = new RefundEvent(orderId, paymentId, seatsBooked.size());
            kafkaTemplate.send(REFUND_TOPIC, orderId, refundEvent);
            return ResponseEntity.ok().body("Your session expired but payment was received. A refund will be processed shortly.");
        }
    }
}
