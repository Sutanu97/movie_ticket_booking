package com.projects.MovieTicketBookingSystem.dao;

import com.projects.MovieTicketBookingSystem.entity.Movie;
import com.projects.MovieTicketBookingSystem.entity.Show;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface MovieSearchDao {

    public List<Movie> getMovies(String city, LocalDateTime lowerBound, LocalDateTime upperBound);

    Map<LocalDate, List<Show>> getShowsForCityAndMovie(String city, String movieName);

    List<String> getAvailableSeatsForShow(Integer showId);
}
