package com.projects.MovieTicketBookingSystem.manager;

import com.projects.MovieTicketBookingSystem.entity.Movie;
import com.projects.MovieTicketBookingSystem.entity.Show;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface MovieSearchManager {
    public List<Movie> getMovies(String city);

    Map<LocalDate, List<Show>> getShowsForCityAndMovie(String city, String movieName);

    List<Movie> getMovies(String city, LocalDateTime fromTime, LocalDateTime toTime);

    List<String> getAvailableSeatsForShow(Integer showId);
}
