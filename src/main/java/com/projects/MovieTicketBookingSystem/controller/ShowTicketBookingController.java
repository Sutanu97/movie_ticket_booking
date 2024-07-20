package com.projects.MovieTicketBookingSystem.controller;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dao.UserRepository;
import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.dto.RazorPayOrderDto;
import com.projects.MovieTicketBookingSystem.dto.TicketBookingConfig;
import com.projects.MovieTicketBookingSystem.entity.RazorPayOrderStatus;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import com.projects.MovieTicketBookingSystem.util.NotificationUtil;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ShowTicketBookingController {

    private TicketBookingManager ticketBookingManager;
    private UserRepository userRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTicketBookingController.class);

    public ShowTicketBookingController(TicketBookingManager ticketBookingManager, UserRepository userRepository) {
        this.ticketBookingManager = ticketBookingManager;
        this.userRepository = userRepository;
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
        LOGGER.debug("In method updateRazorpayOrder");
        String orderId = ticketBookingConfig.getOrderId();
        Show show = ticketBookingConfig.getShow();
        Set<String> seatsBooked =  Arrays.stream(ticketBookingConfig.getSeats().split(" ")).collect(Collectors.toSet());
        if(ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())){
            LOGGER.info("Trying to acquire the lock");
            synchronized (TicketBookingConstants.LOCK){
                LOGGER.info("Lock has been acquired");
                if(ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())){
                    LOGGER.info("Going to update the Razorpay order with status "+TicketBookingConstants.SUCCESS);
                    ticketBookingManager.updateRazorpayOrder(orderId, TicketBookingConstants.SUCCESS);
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    String emailId = this.userRepository.findByUsername(username).get().getEmailId();
                    NotificationUtil.sendConfirmationEmail(orderId, show, emailId, seatsBooked);
                    this.ticketBookingManager.addToHistory(show, orderId, seatsBooked);
                    return ResponseEntity.ok().body("Booking has been confirmed!");
                }else{
                    String msg = "The Payment has already expired. Please try booking again.";
                    LOGGER.info(msg);
                    return ResponseEntity.ok().body(msg);
                }
            }
        }else{
            String msg = "The Payment has already expired. Please try booking again.";
            LOGGER.info(msg);
            return ResponseEntity.ok().body(msg);
        }
    }
}
