package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "booking_history")
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_booking_history_id")
    private Integer pkBookingHistoryId;

    @ManyToOne
    @JoinColumn(name = "fk_user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "fk_show_id")
    private Show show;

    @Column(name = "fk_razorpay_order_id")
    private String razorpayOrder;

    private String seats;

    private Integer cost;

    public Integer getPkBookingHistoryId() {
        return pkBookingHistoryId;
    }

    public void setPkBookingHistoryId(Integer pkBookingHistoryId) {
        this.pkBookingHistoryId = pkBookingHistoryId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public String getRazorpayOrder() {
        return razorpayOrder;
    }

    public void setRazorpayOrder(String razorpayOrder) {
        this.razorpayOrder = razorpayOrder;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }
}
