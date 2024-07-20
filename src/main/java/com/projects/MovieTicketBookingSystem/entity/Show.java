package com.projects.MovieTicketBookingSystem.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "movie_show")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_movie_show_id")
    private Integer pkMovieShowId;

    @ManyToOne
    @JoinColumn(name = "fk_movie_id")
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "fk_multiplex_id")
    private Multiplex multiplex;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "fk_screen_id", referencedColumnName = "screenId",insertable=false, updatable=false),
            @JoinColumn(name = "fk_multiplex_id", referencedColumnName = "fkMultiplexId", insertable = false,updatable = false)
    })

    private Screen screen;

    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    public Integer getPkMovieShowId() {
        return pkMovieShowId;
    }

    public void setPkMovieShowId(Integer pkMovieShowId) {
        this.pkMovieShowId = pkMovieShowId;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Multiplex getMultiplex() {
        return multiplex;
    }

    public void setMultiplex(Multiplex multiplex) {
        this.multiplex = multiplex;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
