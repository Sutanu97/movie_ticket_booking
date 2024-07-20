package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "multiplex")
public class Multiplex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_multiplex_id")
    private Integer pkMultiplexId;
    private String name;
    private String city;
    private String address;

    public Integer getPkMultiplexId() {
        return pkMultiplexId;
    }

    public void setPkMultiplexId(Integer pkMultiplexId) {
        this.pkMultiplexId = pkMultiplexId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
