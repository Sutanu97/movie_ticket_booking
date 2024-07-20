package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_user_id")
    private Integer pkUserId;
    private String username;
    @Size(min = 4)
    private String password;
    @Digits(integer = 10, fraction = 0)
    private Long contact;
    @Column(name = "email_id")
    @Email
    private String emailId;
    private String roles;

    public Integer getPkUserId() {
        return pkUserId;
    }

    public void setPkUserId(Integer pkUserId) {
        this.pkUserId = pkUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getContact() {
        return contact;
    }

    public void setContact(Long contact) {
        this.contact = contact;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
}
