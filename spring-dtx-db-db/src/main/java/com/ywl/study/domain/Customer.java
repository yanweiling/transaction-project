package com.ywl.study.domain;

/**
 * Created by mavlarn on 2018/1/20.
 */

public class Customer {

    private Long id;

    private String username;

    /*余额*/
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
