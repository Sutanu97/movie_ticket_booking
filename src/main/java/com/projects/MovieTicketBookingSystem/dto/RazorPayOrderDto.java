package com.projects.MovieTicketBookingSystem.dto;

import java.io.Serializable;

public class RazorPayOrderDto implements Serializable {
    String paymentInitMsg;
    String orderId;
    Double amount;

    public RazorPayOrderDto(String paymentInitMsg, String orderId, Double amount) {
        this.paymentInitMsg = paymentInitMsg;
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getPaymentInitMsg() {
        return paymentInitMsg;
    }

    public String getOrderId() {
        return orderId;
    }

    public Double getAmount() {
        return amount;
    }
}
