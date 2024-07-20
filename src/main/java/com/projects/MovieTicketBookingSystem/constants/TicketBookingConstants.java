package com.projects.MovieTicketBookingSystem.constants;

public class TicketBookingConstants {

    public static final String SUCCESS = "Success";
    public static final Integer MAX_SEATS = 10;
    public static final String BOOKING_STATUS = "BOOKING_STATUS";
    public static final String SEATS_RESERVATION_STATUS = "SEATS_RESERVATION_STATUS";
    public static final String FAILED = "Failed";
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    public static final String SEATS_ALREADY_TAKEN_MESSAGE = "Sorry! The desired seats are already taken.";
    public static final String HIGH_TRAFFIC_MESSAGE = "Sorry! We could not book your tickets due to heavy traffic. Please try again later";
    public static final Integer LOCK_TIME = 6;
    public static final Long SLEEP_TIMEOUT = 300000l;
    public static final String DATE_TIME_FORMATTER= "yyyy-MM-dd HH:mm:ss";
    public static final String RAZORPAY_KEY_ID = "razorpay_key_id";
    public static final String RAZORPAY_SECRET_ID = "razorpay_key_secret";
    public static final String RAZORPAY_ORDER = "razorpay_order";
    public static final String EMAIL_FROM = "bhattacharyasutanu97@gmail.com";
    public static final int TICKET_PRICE = 10000;
    public static final Object LOCK = new Object();
    public static final int NUMBER_OF_THREADS = 5;
    public static final String RAZORPAY_AMOUNT = "amount";
}
