package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dao.TicketBookingDao;
import com.projects.MovieTicketBookingSystem.daoImpl.TicketBookingDaoImpl;
import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import com.projects.MovieTicketBookingSystem.util.TicketBookingUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TicketBookingManagerImpl implements TicketBookingManager {

    private TicketBookingDao ticketBookingDao;
    private TicketBookingUtil ticketBookingUtil;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    Lock lock = new ReentrantLock();

    Logger LOGGER = LoggerFactory.getLogger(TicketBookingDaoImpl.class);

    public TicketBookingManagerImpl(TicketBookingDao ticketBookingDao, TicketBookingUtil ticketBookingUtil) {
        this.ticketBookingDao = ticketBookingDao;
        this.ticketBookingUtil = ticketBookingUtil;
    }


    /*TODO :
     * bookShow will call the following private methods :
     *       reseveSeats() : to get the lock and book seats
     *       initiatePayment() : to all the Razorpay API and get the order ID.
     *
     *   After that it will start a new thread which will :
     *       wait for 5 mins.
     *       send a call method updateBookingStatus(String status) to the orders table and fail the order
     *       unreserve the reserved seats associated with the order id.
     *       release the lock
     *   start the thread.start()
     *   return the order id back to the caller.
     *
     *  Create an API endpoint in ticketBooking, for accepting the status of payment from the client which has
     * been sent to it by Razorpay.
     * If it is SUCCESS, then call the updateBookingStatus(String status) method and update
     * the orders table and the booking history table.
     * Send an interrupt call to the thread initiated in the booking method.
     *
     *
     *
     * */
    @Override
    public Map<String, String> bookShow(Show show, Set<String> seatsSet) {
        LOGGER.debug("In method bookShow");
        Integer showId = show.getPkMovieShowId();
        Map<String, String> resultMap = new HashMap<>();
        if(showId == null){
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            return resultMap;
        }
        resultMap = reserveSeatsForBooking(show, seatsSet);
        if (resultMap != null && !resultMap.get(TicketBookingConstants.SEATS_RESERVATION_STATUS).equals(TicketBookingConstants.SUCCESS)) {
            resultMap.put(TicketBookingConstants.BOOKING_STATUS, TicketBookingConstants.FAILED);
            return resultMap;
        }

        Order order = initiatePayment(showId, seatsSet);
        String orderId = TicketBookingUtil.getOrderIdFromOrder(order);
        this.ticketBookingDao.addRazorpayOrder(orderId, showId, seatsSet.size());
        resultMap.put(TicketBookingConstants.RAZORPAY_ORDER, orderId);
        resultMap.put(TicketBookingConstants.RAZORPAY_AMOUNT,String.valueOf(TicketBookingConstants.TICKET_PRICE * seatsSet.size()));
        return resultMap;
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

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId,
                    razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", seatsOccupied.size()*TicketBookingConstants.TICKET_PRICE);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_e2225");
            Order order = razorpayClient.orders.create(orderRequest);
            LOGGER.info("Order is created : " + order);
            ticketBookingUtil.beginTimeoutThread(this, showId, order, seatsOccupied);
            return order;
        } catch (RazorpayException e) {
            ticketBookingDao.reallocateSeats(showId, seatsOccupied);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private Map<String, String> reserveSeatsForBooking(Show show, Set<String> seatsSet) {
        LOGGER.debug("In method reserveSeatsForBooking");
        Integer showId = show.getPkMovieShowId();
        Map<String, String> bookingStatus = null;
        if (TicketBookingUtil.isBookingLimitCrossed(seatsSet)) {
            bookingStatus = new HashMap<>();
            bookingStatus.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
            bookingStatus.put(TicketBookingConstants.ERROR_MESSAGE, "Seat count exceeded. Maximum booking capacity is " + TicketBookingConstants.MAX_SEATS);
            return bookingStatus;
        }

        try {
            if (lock.tryLock(TicketBookingConstants.LOCK_TIME, TimeUnit.MINUTES)) {
                boolean seatAvailabilityStatus = TicketBookingUtil.areSeatsAvailable(ticketBookingDao.getTotalSeats(show), ticketBookingDao.getAvailableSeats(showId), seatsSet);
                if (!seatAvailabilityStatus) {
                    LOGGER.info(TicketBookingConstants.SEATS_ALREADY_TAKEN_MESSAGE);
                    bookingStatus = new HashMap<>();
                    bookingStatus.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
                    bookingStatus.put(TicketBookingConstants.ERROR_MESSAGE, TicketBookingConstants.SEATS_ALREADY_TAKEN_MESSAGE);
                    lock.unlock();
                    return bookingStatus;
                }

                try {
                    LOGGER.info("Seats selected are available and going ahead with booking");

                    ticketBookingDao.reserveSeats(showId, seatsSet);//reserving those seats
                    bookingStatus = new HashMap<>();
                    bookingStatus.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.SUCCESS);
                    return bookingStatus;
                } finally {
                    if (!bookingStatus.get(TicketBookingConstants.SEATS_RESERVATION_STATUS).equals(TicketBookingConstants.SUCCESS)) {
                        ticketBookingDao.reallocateSeats(showId, seatsSet);
                        lock.unlock();
                    }
                }
            } else {
                LOGGER.info(TicketBookingConstants.HIGH_TRAFFIC_MESSAGE);
                bookingStatus = new HashMap<>();
                bookingStatus.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
                bookingStatus.put(TicketBookingConstants.ERROR_MESSAGE, TicketBookingConstants.HIGH_TRAFFIC_MESSAGE);
                return bookingStatus;
            }
        } catch (Exception e) {
            LOGGER.error("Thread was interrupted while reserving seats for the show", e);
            bookingStatus = new HashMap<>();
            bookingStatus.put(TicketBookingConstants.SEATS_RESERVATION_STATUS, TicketBookingConstants.FAILED);
            bookingStatus.put(TicketBookingConstants.ERROR_MESSAGE, "Thread was interrupted while reserving seats for the show");
            return bookingStatus;
        }
    }
}
