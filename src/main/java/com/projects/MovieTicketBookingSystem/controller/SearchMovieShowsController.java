package com.projects.MovieTicketBookingSystem.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants.DATE_TIME_FORMATTER;
import com.projects.MovieTicketBookingSystem.entity.Movie;
import com.projects.MovieTicketBookingSystem.entity.Show;
import com.projects.MovieTicketBookingSystem.manager.MovieSearchManager;

@RestController
public class SearchMovieShowsController {

    @Autowired
    private MovieSearchManager movieSearchManager;

    public SearchMovieShowsController(MovieSearchManager movieSearchManager) {
        this.movieSearchManager = movieSearchManager;
    }

    @GetMapping("/movies")
    public List<Movie> getAllMovies(@RequestParam String city){
        return this.movieSearchManager.getMovies(city);
    }

    @GetMapping(path = "/movies", params = {"lowerBound", "upperBound"})
    public List<Movie> getAllMovies(@RequestParam String city,
                                    @RequestParam("lowerBound") String lowerBound,
                                    @RequestParam("upperBound") String upperBound){

        LocalDateTime from = LocalDateTime.parse(lowerBound, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
        LocalDateTime to = LocalDateTime.parse(upperBound, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
        return this.movieSearchManager.getMovies(city, from, to);
    }

    @GetMapping("/shows")
    public Map<LocalDate, List<Show>> getShows(@RequestParam String city, @RequestParam String movieName){
        return this.movieSearchManager.getShowsForCityAndMovie(city, movieName);
    }

    @GetMapping("/shows/{showId}/seats")
    public List<String> getAvailableSeatsForShow(@PathVariable Integer showId){
        return this.movieSearchManager.getAvailableSeatsForShow(showId);
    }
}
