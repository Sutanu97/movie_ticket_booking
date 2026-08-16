package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dto.RefundEvent;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes refund events from the "booking.refund" Kafka topic.
 *
 * This handles the race condition where a user's payment is captured by Razorpay
 * after the booking session has already expired and been marked FAILED by
 * BookingTimeoutService. In this case, the user must be refunded.
 *
 * Kafka guarantees at-least-once delivery; Razorpay's refund API is idempotent
 * on the payment ID, so duplicate processing is safe.
 */
@Service
public class RefundConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundConsumer.class);

    private final RazorpayClient razorpayClient;

    public RefundConsumer(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    @KafkaListener(topics = "booking.refund", groupId = "refund-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void processRefund(RefundEvent event) {
        LOGGER.warn("Refund event received for orderId: {} | paymentId: {}",
                event.getOrderId(), event.getRazorpayPaymentId());
        try {
            JSONObject refundRequest = new JSONObject();
            // Full refund: seat count * ticket price (amount in paise)
            refundRequest.put("amount", event.getSeatCount() * TicketBookingConstants.TICKET_PRICE);
            refundRequest.put("speed", "normal");

            // Razorpay refund call — idempotent on paymentId + amount
            razorpayClient.payments.refund(event.getRazorpayPaymentId(), refundRequest);
            LOGGER.info("Refund successfully initiated for orderId: {} | paymentId: {}",
                    event.getOrderId(), event.getRazorpayPaymentId());
        } catch (RazorpayException e) {
            LOGGER.error("Razorpay refund failed for orderId: {} | paymentId: {}. Will retry via Kafka.",
                    event.getOrderId(), event.getRazorpayPaymentId(), e);
            // Re-throwing causes Kafka to retry based on the consumer's retry/backoff config
            throw new RuntimeException("Refund failed for order: " + event.getOrderId(), e);
        }
    }
}
