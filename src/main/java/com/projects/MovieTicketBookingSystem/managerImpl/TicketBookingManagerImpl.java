package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dao.TicketBookingDao;
import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.dto.TicketBookingConfig;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import com.projects.MovieTicketBookingSystem.util.TicketBookingUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TicketBookingManagerImpl implements TicketBookingManager {

    private final TicketBookingDao ticketBookingDao;
    private final TicketBookingUtil ticketBookingUtil;
    private final RedissonClient redissonClient;
    private final RazorpayClient razorpayClient;

    Logger LOGGER = LoggerFactory.getLogger(TicketBookingManagerImpl.class);

    public TicketBookingManagerImpl(TicketBookingDao ticketBookingDao, TicketBookingUtil ticketBookingUtil,
            RedissonClient redissonClient, RazorpayClient razorpayClient) {
        this.ticketBookingDao = ticketBookingDao;
        this.ticketBookingUtil = ticketBookingUtil;
        this.redissonClient = redissonClient;
        this.razorpayClient = razorpayClient;
    }

    @Override
    public Map<String, String> bookShow(Show show, Set<String> seatsSet) {
        LOGGER.debug("In method bookShow");
        Integer showId = show.getPkMovieShowId();
        Map<String, String> resultMap = new HashMap<>();
        if (showId == null) {
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            return resultMap;
        }

        if (TicketBookingUtil.isBookingLimitCrossed(seatsSet)) {
            resultMap.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
            resultMap.put(TicketBookingConstants.ERROR_MESSAGE,
                    "Seat count exceeded. Maximum booking capacity is " + TicketBookingConstants.MAX_SEATS);
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            return resultMap;
        }

        RLock lock = redissonClient.getLock("lock:show:" + showId);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!isLocked) {
                resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
                resultMap.put(TicketBookingConstants.ERROR_MESSAGE, "Booking request timed out. Please try again.");
                return resultMap;
            }

            boolean seatAvailabilityStatus = TicketBookingUtil.areSeatsAvailable(ticketBookingDao.getTotalSeats(show),
                    ticketBookingDao.getAvailableSeats(showId), seatsSet);
            if (!seatAvailabilityStatus) {
                LOGGER.info(TicketBookingConstants.SEATS_ALREADY_TAKEN_MESSAGE);
                resultMap.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
                resultMap.put(TicketBookingConstants.ERROR_MESSAGE, TicketBookingConstants.SEATS_ALREADY_TAKEN_MESSAGE);
                resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
                return resultMap;
            }

            LOGGER.info("Seats selected are available and going ahead with booking");
            ticketBookingDao.reserveSeats(showId, seatsSet);
            resultMap.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.SUCCESS);

        } catch (InterruptedException e) {
            LOGGER.error("Thread was interrupted while reserving seats for the show", e);
            resultMap.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
            resultMap.put(TicketBookingConstants.ERROR_MESSAGE,
                    "Thread was interrupted while reserving seats for the show");
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            Thread.currentThread().interrupt();
            return resultMap;
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }

        try {
            Order order = initiatePayment(showId, seatsSet);
            String orderId = TicketBookingUtil.getOrderIdFromOrder(order);
            this.ticketBookingDao.addRazorpayOrder(orderId, showId, seatsSet.size());
            resultMap.put(TicketBookingConstants.RAZORPAY_ORDER, orderId);
            resultMap.put(TicketBookingConstants.RAZORPAY_AMOUNT,
                    String.valueOf(TicketBookingConstants.TICKET_PRICE * seatsSet.size()));

            String payload = orderId + "|" + showId + "|" + String.join(",", seatsSet);
            RQueue<String> queue = redissonClient.getQueue("bookingTimeoutQueue");
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
            delayedQueue.offer(payload, TicketBookingConstants.SLEEP_TIMEOUT, TimeUnit.MILLISECONDS);

            return resultMap;
        } catch (Exception e) {
            LOGGER.error("Failed to initiate payment", e);
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            resultMap.put(TicketBookingConstants.ERROR_MESSAGE, "Failed to initiate payment. Please try again.");
            return resultMap;
        }
    }

    @Override
    public void updateRazorpayOrder(String orderID, String status) {
        this.ticketBookingDao.updateRazorpayOrder(orderID, status);
    }

    @Override
    public void reallocateSeats(Integer showId, Set<String> seats) {
        this.ticketBookingDao.reallocateSeats(showId, seats);
    }

    @Override
    public void addToHistory(Show show, String orderId, Set<String> seatsBooked) {
        this.ticketBookingDao.addToHistory(show, orderId, seatsBooked);
    }

    @Override
    public String fetchRazorpayOrderStatus(String orderId) {
        return this.ticketBookingDao.fetchRazorPayOrderStatus(orderId);
    }

    @Override
    public List<BookingSummary> getBookingsForUser(String username) {
        return this.ticketBookingDao.getBookingsForUser(username);
    }

    private Order initiatePayment(Integer showId, Set<String> seatsOccupied) {
        LOGGER.debug("In method initiatePayment");
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", seatsOccupied.size() * TicketBookingConstants.TICKET_PRICE);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());
            Order order = razorpayClient.orders.create(orderRequest);
            LOGGER.info("Order is created : " + order);
            return order;
        } catch (RazorpayException e) {
            ticketBookingDao.reallocateSeats(showId, seatsOccupied);
            throw new RuntimeException(e);
        }
    }
}
