package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class MultiplexScreenPk {

    protected Integer screenId;
    protected Integer fkMultiplexId;

    public MultiplexScreenPk(){}

    public MultiplexScreenPk(Integer screenId, Integer multiplexId) {
        this.screenId = screenId;
        this.fkMultiplexId = multiplexId;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public Integer getFkMultiplexId() {
        return fkMultiplexId;
    }
}
