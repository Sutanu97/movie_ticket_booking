package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.dao.MovieSearchDao;
import com.projects.MovieTicketBookingSystem.entity.Movie;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.MovieSearchManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MovieSearchManagerImpl implements MovieSearchManager {

    private MovieSearchDao movieSearchDao;

    public MovieSearchManagerImpl(MovieSearchDao movieSearchDao) {
        this.movieSearchDao = movieSearchDao;
    }

    @Override
    public List<Movie> getMovies(String city){
        return this.movieSearchDao.getMovies(city, null, null);
    }

    @Override
    public Map<LocalDate, List<Show>> getShowsForCityAndMovie(String city, String movieName) {
        return  this.movieSearchDao.getShowsForCityAndMovie(city,movieName);
    }

    @Override
    public List<Movie> getMovies(String city, LocalDateTime fromTime, LocalDateTime toTime) {
        return this.movieSearchDao.getMovies(city, fromTime, toTime);
    }

    @Override
    public List<String> getAvailableSeatsForShow(Integer showId) {
        return this.movieSearchDao.getAvailableSeatsForShow(showId);
    }
}
