package com.projects.MovieTicketBookingSystem.daoImpl;

import com.projects.MovieTicketBookingSystem.dao.MovieSearchDao;
import com.projects.MovieTicketBookingSystem.entity.Movie;
import com.projects.MovieTicketBookingSystem.entity.Show;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MovieSearchDaoImpl implements MovieSearchDao {

    public static final Logger LOGGER = LoggerFactory.getLogger(MovieSearchDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<Movie> getMovies(String city, LocalDateTime lowerBound, LocalDateTime upperBound) {
        LOGGER.debug("In method getMovies");
        List<Movie> movies = new ArrayList<>();
        StringBuilder query = new StringBuilder("select movie from Show s where s.multiplex.city = :city ") ;
        if(lowerBound != null && upperBound != null){
            query.append("and s.startTime >= :startTime and s.endTime <= :endTime");
        }
        TypedQuery<Movie> jpql = entityManager.createQuery(query.toString(), Movie.class);
        jpql.setParameter("city",city);
        if(lowerBound != null && upperBound != null){
            jpql.setParameter("startTime",lowerBound);
            jpql.setParameter("endTime",upperBound);
        }
        return jpql.getResultList();
    }

    @Override
    public Map<LocalDate, List<Show>> getShowsForCityAndMovie(String city, String movieName) {
        LOGGER.debug("In method getShowsForCityAndMovie");
        Map<LocalDate, List<Show>> dateVsShowsMap = null;
        String query = "select s from Show s where s.multiplex.city = :city and s.movie.name = :movie";
        TypedQuery<Show> jpql = entityManager.createQuery(query, Show.class);
        jpql.setParameter("city", city);
        jpql.setParameter("movie", movieName);
        List<Show> rs = jpql.getResultList();
        if(rs != null){
            dateVsShowsMap = new HashMap<>();
            for(Show s : rs){
                LocalDate showDate = s.getStartTime().toLocalDate();
                List<Show> shows = dateVsShowsMap.getOrDefault(showDate, new ArrayList<Show>());
                shows.add(s);
                dateVsShowsMap.put(showDate, shows);
            }
        }
        return dateVsShowsMap;
    }

    @Override
    public List<String> getAvailableSeatsForShow(Integer showId) {
        LOGGER.debug("In method getAvailableSeatsForShow");
        String query = "select sa.seatsLeft from SeatAvailability sa where sa.show.pkMovieShowId = :showId";
        Query jpql = entityManager.createQuery(query);
        jpql.setParameter("showId", showId);
        return (List<String>) jpql.getResultList();
    }


}
