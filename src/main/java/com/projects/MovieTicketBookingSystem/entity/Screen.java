package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "multiplex_screen")
public class Screen {

    @EmbeddedId
    private MultiplexScreenPk multiplexScreenPk;
    private String seatingCapacity;

    public MultiplexScreenPk getMultiplexScreenPk() {
        return multiplexScreenPk;
    }

    public void setMultiplexScreenPk(MultiplexScreenPk multiplexScreenPk) {
        this.multiplexScreenPk = multiplexScreenPk;
    }

    public String getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(String seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }
}
