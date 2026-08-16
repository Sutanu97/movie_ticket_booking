package com.projects.MovieTicketBookingSystem.util;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.daoImpl.TicketBookingDaoImpl;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;

import com.razorpay.Order;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class TicketBookingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketBookingDaoImpl.class);

    public static boolean isBookingLimitCrossed(Set<String> seats) {
        LOGGER.debug("In method isBookingLimitCrossed");
        return seats.size() > TicketBookingConstants.MAX_SEATS;
    }

    public static boolean areSeatsAvailable(Set<String> totalSeats, Set<String> availableSeats,
            Set<String> claimedSeats) {
        LOGGER.debug("In method areSeatsAvailable");
        totalSeats.removeAll(availableSeats);
        for (String claimedSeat : claimedSeats) {
            if (totalSeats.contains(claimedSeat)) {
                return false;
            }
        }
        return true;
    }

    public static String getSeatsAfterReservation(String[] seatArr, Set<String> totalSeats) {
        LOGGER.debug("In method reserveSeats");
        Arrays.asList(seatArr).forEach(totalSeats::remove);
        return String.join(" ", totalSeats);
    }

    public static String getOrderIdFromOrder(Order order) {
        LOGGER.debug("In method getOrderIdFromOrder");
        JSONObject orderObj = order.toJson();
        return orderObj.optString("id");
    }

}
