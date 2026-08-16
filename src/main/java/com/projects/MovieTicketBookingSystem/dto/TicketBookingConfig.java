package com.projects.MovieTicketBookingSystem.dto;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.entity.Show;

import java.io.Serializable;

public class TicketBookingConfig implements Serializable {

    private String orderId;
    private Show show;
    private String seats;
    private Integer cost;
    // Razorpay callback fields — sent by frontend after payment, used for server-side verification
    private String razorpayPaymentId;
    private String razorpaySignature;

    // No-arg constructor required for Jackson deserialization
    public TicketBookingConfig() {}

    public TicketBookingConfig(String orderId, Show show, String seats) {
        this.orderId = orderId;
        this.show = show;
        this.seats = seats;
    }

    public Show getShow() {
        return show;
    }

    public String getSeats() {
        return seats;
    }

    public String getOrderId() {
        return orderId;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost() {
        cost = seats.split(" ").length * TicketBookingConstants.TICKET_PRICE/100;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }
}

