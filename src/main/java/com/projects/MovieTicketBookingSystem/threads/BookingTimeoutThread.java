package com.projects.MovieTicketBookingSystem.threads;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.entity.RazorPayOrderStatus;
import com.projects.MovieTicketBookingSystem.manager.TicketBookingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BookingTimeoutThread implements Runnable{

    private String orderId;
    private Set<String> seatsBooked;
    private Integer showId;
    private TicketBookingManager ticketBookingManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingTimeoutThread.class);

    public BookingTimeoutThread(){}

    public BookingTimeoutThread(TicketBookingManager ticketBookingManager) {
        this.ticketBookingManager = ticketBookingManager;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setSeatsBooked(Set<String> seatsBooked) {
        this.seatsBooked = seatsBooked;
    }

    public void setShowId(Integer showId) {
        this.showId = showId;
    }

    @Override
    public void run() {
        LOGGER.debug("In method run");
        try {
            LOGGER.info("Thread created to fail the order with ID : "+orderId);
            Thread.currentThread().sleep(TicketBookingConstants.SLEEP_TIMEOUT);
            LOGGER.info("Thread awakened and now attempting to fail the order with ID : "+orderId);
            if(ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())){
                LOGGER.debug("Trying to acquire lock");
                synchronized (TicketBookingConstants.LOCK){
                    LOGGER.debug("Lock has been acquired.");
                    if(ticketBookingManager.fetchRazorpayOrderStatus(orderId).equals(RazorPayOrderStatus.Created.toString())){
                        LOGGER.info("Proceeding to update the Razorpay order as failed. ");
                        ticketBookingManager.updateRazorpayOrder(orderId, TicketBookingConstants.FAILED);
                        ticketBookingManager.reallocateSeats(showId, seatsBooked);
                    }else{
                        String msg = "The Payment has already been completed.";
                        LOGGER.info(msg);
                    }
                }
            }else{
                String msg = "The Payment has already been completed.";
                LOGGER.info(msg);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Thread has been interrupted");
        }catch (Exception e) {
            LOGGER.error("Exception caught while attempting to fail the order with ID : "+orderId,
                    e);
        }
    }
}
