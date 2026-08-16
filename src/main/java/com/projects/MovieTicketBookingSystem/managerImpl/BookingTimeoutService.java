package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.entity.RazorPayOrderStatus;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingTimeoutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingTimeoutService.class);

    private final RedissonClient redissonClient;
    private final TicketBookingManager ticketBookingManager;

    public BookingTimeoutService(RedissonClient redissonClient, TicketBookingManager ticketBookingManager) {
        this.redissonClient = redissonClient;
        this.ticketBookingManager = ticketBookingManager;
    }

    @PostConstruct
    public void init() {
        new Thread(() -> {
            RBlockingQueue<String> queue = redissonClient.getBlockingQueue("bookingTimeoutQueue");
            while (true) {
                try {
                    // This blocks until an element is moved from delayed queue to the actual queue
                    String payload = queue.take();
                    processTimeout(payload);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("BookingTimeoutService interrupted", e);
                    break;
                } catch (Exception e) {
                    LOGGER.error("Exception in BookingTimeoutService", e);
                }
            }
        }).start();
    }

    private void processTimeout(String payload) {
        String[] parts = payload.split("\\|");
        String orderId = parts[0];
        Integer showId = Integer.parseInt(parts[1]);
        Set<String> seatsBooked = Arrays.stream(parts[2].split(",")).collect(Collectors.toSet());

        LOGGER.info("Attempting to fail the order with ID : " + orderId);
        
        if (ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())) {
            RLock lock = redissonClient.getLock("lock:order:" + orderId);
            try {
                lock.lock();
                if (ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())) {
                    LOGGER.info("Proceeding to update the Razorpay order as failed.");
                    ticketBookingManager.updateRazorpayOrder(orderId, TicketBookingConstants.FAILED);
                    ticketBookingManager.reallocateSeats(showId, seatsBooked);
                } else {
                    LOGGER.info("The Payment has already been completed.");
                }
            } finally {
                lock.unlock();
            }
        } else {
            LOGGER.info("The Payment has already been completed.");
        }
    }
}
