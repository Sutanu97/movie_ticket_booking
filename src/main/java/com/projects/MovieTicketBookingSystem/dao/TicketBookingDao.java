package com.projects.MovieTicketBookingSystem.dao;

import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.entity.Show;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface TicketBookingDao {
    HashSet<String> getTotalSeats(Show show);

    void reserveSeats(Integer showId, Set<String> seats);

    Set<String> getAvailableSeats(java.lang.Integer showId);

    void updateRazorpayOrder(String orderID, String status);

    void addToHistory(Show show, String orderId, Set<String> seatsBooked);

    String fetchRazorPayOrderStatus(String orderId);

    void addRazorpayOrder(String orderId, Integer showId, int seatsCount);

    void reallocateSeats(Integer showId, Set<String> seats);

    List<BookingSummary> getBookingsForUser(String username);
}
