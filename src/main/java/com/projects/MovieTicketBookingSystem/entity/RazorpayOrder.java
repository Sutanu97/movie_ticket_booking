package com.projects.MovieTicketBookingSystem.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class RazorpayOrder implements Serializable {

    @Id
    private String pkRazorpayOrderId;

    @ManyToOne
    @JoinColumn(name = "fk_show_id")
    private Show show;

    @ManyToOne
    @JoinColumn(name = "fk_user_id")
    private User user;

    @Column(name = "order_status")
    @Enumerated(value = EnumType.STRING)
    private RazorPayOrderStatus orderStatus;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "cost")
    private Integer cost;

    public String getPkRazorpayOrderId() {
        return pkRazorpayOrderId;
    }

    public void setPkRazorpayOrderId(String pkRazorpayOrderId) {
        this.pkRazorpayOrderId = pkRazorpayOrderId;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RazorPayOrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(RazorPayOrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }
}
