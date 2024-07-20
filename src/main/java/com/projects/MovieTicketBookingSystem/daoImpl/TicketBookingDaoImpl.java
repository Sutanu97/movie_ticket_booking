package com.projects.MovieTicketBookingSystem.daoImpl;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.dao.TicketBookingDao;
import com.projects.MovieTicketBookingSystem.dto.BookingSummary;
import com.projects.MovieTicketBookingSystem.dto.UserPrincipal;
import com.projects.MovieTicketBookingSystem.entity.*;
import com.projects.MovieTicketBookingSystem.util.UserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TicketBookingDaoImpl implements TicketBookingDao {

    @PersistenceContext
    private EntityManager entityManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketBookingDaoImpl.class);


    @Override
    public HashSet<String> getTotalSeats(Show show) {
        LOGGER.debug("In method getTotalSeats");
        String query = "select seatingCapacity from Screen s where s.multiplexScreenPk.fkMultiplexId = :multiplexId and s.multiplexScreenPk.screenId = :screenId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("multiplexId", show.getMultiplex().getPkMultiplexId());
        jpql.setParameter("screenId", show.getScreen().getMultiplexScreenPk().getScreenId());
        List<String> rs = jpql.getResultList();
        return new HashSet<String>(Arrays.stream(rs.get(0).split(" ")).collect(Collectors.toSet()));
    }

    @Override
    @Transactional
    public void reserveSeats(Integer showId, Set<String> seats) {
        LOGGER.debug("In method seatsAfterReservation");
        Set<String> availableSeats = getAvailableSeats(showId);
        availableSeats.removeAll(seats);
        String query = "update SeatAvailability sa set " +
                " seatsLeft = :seatsLeft" +
                " where show.pkMovieShowId = :showId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("seatsLeft", String.join(" ",availableSeats));
        jpql.setParameter("showId", showId);
        jpql.executeUpdate();
    }

    @Override
    public Set<String> getAvailableSeats(Integer showId) {
        LOGGER.debug("In method getAvailableSeats");
        Set<String> availableSeats = new HashSet<>();
        String query = "select sa.seatsLeft from  SeatAvailability sa where sa.show.pkMovieShowId = :showId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("showId", showId);
        List<String> seatsRs = jpql.getResultList();
        if(seatsRs.size() > 0){
            Set<String> seatsLeft = Arrays.stream(seatsRs.get(0).split(" "))
                    .collect(Collectors.toSet());
            return seatsLeft;
        }
        return null;
    }

    @Override
    @Transactional
    public void updateRazorpayOrder(String orderID, String status) {
        LOGGER.debug("In method updateRazorpayOrder");
        String query = "update RazorpayOrder set orderStatus = :status where pkRazorpayOrderId = :orderId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("status", RazorPayOrderStatus.valueOf(status));
        jpql.setParameter("orderId", orderID);
        jpql.executeUpdate();
    }

    @Override
    @Transactional
    public void addToHistory(Show show, String orderId, Set<String> seatsBooked) {
        LOGGER.debug("In method addToHistory");
        User user = UserUtil.getUserFromUserPrincipal((UserPrincipal)SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        BookingHistory bookingHistory = new BookingHistory();
        bookingHistory.setSeats(String.join(",", seatsBooked));
        bookingHistory.setShow(show);
        bookingHistory.setRazorpayOrder(orderId);
        bookingHistory.setUser(user);
        bookingHistory.setCost(seatsBooked.size() * TicketBookingConstants.TICKET_PRICE/100);
        entityManager.persist(bookingHistory);
    }

    @Override
    public String fetchRazorPayOrderStatus(String orderId) {
        LOGGER.debug("In method fetchRazorpayOrderStatus");
        String query = "select order.orderStatus from RazorpayOrder order where pkRazorpayOrderId = :orderId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("orderId", orderId);
        List<RazorPayOrderStatus> status = jpql.getResultList();
        return status.get(0).toString();
    }

    @Override
    @Transactional
    public void addRazorpayOrder(String orderId, Integer showId, int seatsCount) {
        LOGGER.debug("In method addRazorpayOrder");
        Show show = new Show();
        show.setPkMovieShowId(showId);
        RazorpayOrder razorpayOrder = new RazorpayOrder();
        razorpayOrder.setPkRazorpayOrderId(orderId);
        razorpayOrder.setShow(show);
        razorpayOrder.setCost(seatsCount * TicketBookingConstants.TICKET_PRICE/100);
        razorpayOrder.setUser(UserUtil.getUserFromUserPrincipal((UserPrincipal)SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        razorpayOrder.setOrderStatus(RazorPayOrderStatus.Created);
        razorpayOrder.setCreateTime(LocalDateTime.now());
        razorpayOrder.setUpdateTime(LocalDateTime.now());
        entityManager.persist(razorpayOrder);
    }

    @Override
    @Transactional
    public void reallocateSeats(Integer showId, Set<String> seats) {
        LOGGER.debug("In method reallocateSeats");
        Set<String> availableSeats = getAvailableSeats(showId);
        availableSeats.addAll(seats);
        String query = "update SeatAvailability sa set " +
                " seatsLeft = :seatsLeft" +
                " where show.pkMovieShowId = :showId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("seatsLeft", String.join(" ",availableSeats));
        jpql.setParameter("showId", showId);
        jpql.executeUpdate();
    }

    @Override
    public List<BookingSummary> getBookingsForUser(String username) {
        LOGGER.debug("In method getBookingsForUser");
        List<BookingSummary> bookings = new ArrayList<>();
        String query = "select" +
                " bh.show.movie.name," +
                " bh.show.multiplex.name," +
                " bh.seats, " +
                " bh.show.startTime, " +
                " bh.cost" +
                " from BookingHistory bh where user.username = :username";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("username", username);
        List<Object[]> shows = jpql.getResultList();
        shows.forEach( arr -> {
            BookingSummary summary = new BookingSummary();
            summary.setMovieName(arr[0].toString());
            summary.setMultiplexName(arr[1].toString());
            summary.setSeats(arr[2].toString());
            summary.setShowTime(arr[3].toString());
            summary.setFare(Integer.parseInt(arr[4].toString()));
            bookings.add(summary);
        });
        return bookings;
    }
}
