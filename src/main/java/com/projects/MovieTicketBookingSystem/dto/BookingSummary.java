package com.projects.MovieTicketBookingSystem.dto;

import java.io.Serializable;

public class BookingSummary implements Serializable {
    private String movieName;
    private String multiplexName;
    private String seats;
    private String showTime;
    private Integer fare;

    public BookingSummary() {
    }

    public BookingSummary(String movieName, String multiplexName, String seats, String showTime, Integer fare) {
        this.movieName = movieName;
        this.multiplexName = multiplexName;
        this.seats = seats;
        this.showTime = showTime;
        this.fare = fare;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public String getShowTime() {
        return showTime;
    }

    public void setShowTime(String showTime) {
        this.showTime = showTime;
    }

    public Integer getFare() {
        return fare;
    }

    public void setFare(Integer fare) {
        this.fare = fare;
    }

    public String getMultiplexName() {
        return multiplexName;
    }

    public void setMultiplexName(String multiplexName) {
        this.multiplexName = multiplexName;
    }
}
