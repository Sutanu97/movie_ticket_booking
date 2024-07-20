package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seat_availability")
public class SeatAvailability {
    @Id
    @OneToOne
    @JoinColumn(name = "fk_show_id")
    private Show show;
    private String seatsLeft;

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public String getSeatsLeft() {
        return seatsLeft;
    }

    public void setSeatsLeft(String seatsLeft) {
        this.seatsLeft = seatsLeft;
    }
}
