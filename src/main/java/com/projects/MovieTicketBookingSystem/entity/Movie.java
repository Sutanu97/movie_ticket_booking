package com.projects.MovieTicketBookingSystem.entity;


import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_movie_id")
    private Integer pkMovieId;
    private String name;
    private String description;
    private String language;
    private String genre;
    private String country;
    @Column(name = "release_date")
    private LocalDate releaseDate;

    public Integer getPkMovieId() {
        return pkMovieId;
    }

    public void setPkMovieId(Integer pkMovieId) {
        this.pkMovieId = pkMovieId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }
}
