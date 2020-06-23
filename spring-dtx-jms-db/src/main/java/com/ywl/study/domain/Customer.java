package com.ywl.study.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by mavlarn on 2018/1/20.
 */
@Entity(name = "customer")
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String username;

    /*余额*/
    @Column
    private Integer deposit;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the deposit
     */
    public Integer getDeposit() {
        return deposit;
    }

    /**
     * @param deposit to set
     */
    public void setDeposit(Integer deposit) {
        this.deposit = deposit;
    }
}
