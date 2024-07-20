package com.projects.MovieTicketBookingSystem.errors;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ErrorDto implements Serializable {
    private LocalDateTime timestamp;
    private String url;
    private String error;

    public ErrorDto(LocalDateTime timestamp, String url, String error) {
        this.timestamp = timestamp;
        this.url = url;
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
