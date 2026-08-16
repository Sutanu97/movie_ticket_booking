package com.projects.MovieTicketBookingSystem.dto;

import java.io.Serializable;

/**
 * Event published to the Kafka "booking.refund" topic when a payment is
 * received for an order whose session has already expired.
 * The RefundConsumer picks this up and initiates a Razorpay refund.
 */
public class RefundEvent implements Serializable {

    private String orderId;
    private String razorpayPaymentId;
    private int seatCount;
    private long eventTimestamp;

    // No-arg constructor required for Kafka JSON deserialization
    public RefundEvent() {}

    public RefundEvent(String orderId, String razorpayPaymentId, int seatCount) {
        this.orderId = orderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.seatCount = seatCount;
        this.eventTimestamp = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }
}
