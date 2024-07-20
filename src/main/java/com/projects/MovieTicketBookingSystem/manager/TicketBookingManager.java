package com.projects.MovieTicketBookingSystem.manager;

import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.entity.BookingHistory;
import com.projects.MovieTicketBookingSystem.entity.Show;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TicketBookingManager {

    Map<String, String> bookShow(Show show, Set<String> seats);

    void updateRazorpayOrder(String orderID, String status);

    void reallocateSeats(Integer showId, Set<String> seatsAfterReservation);

    void addToHistory(Show show, String orderId, Set<String> seatsBooked );

    String fetchRazorpayOrderStatus(String orderId);

    List<BookingSummary> getBookingsForUser(String username);
}
