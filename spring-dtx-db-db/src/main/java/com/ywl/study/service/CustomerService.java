package com.ywl.study.service;

import com.ywl.study.domain.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {
    @Autowired
    @Qualifier("userJdbcTemplate")
    private JdbcTemplate userJdbcTemplate;

    @Autowired
    @Qualifier("orderJdbcTemplate")
    private JdbcTemplate orderJdbcTemplate;

    private static final String SQL_UPDATE_DEPOSIT = "update customer set deposit=deposit-? where id=?";
    private static final String SQL_CREATE_ORDER = "insert into customer_order(customer_id,title,amount) values (?,?,?)";

    @Transactional
    //以下写法会报错

    /**
     *
     *  Bean named 'orderDataSource' is expected to be of type 'org.springframework.transaction.TransactionManager'
     *  but was actually of type 'com.zaxxer.hikari.HikariDataSource'
     *  因为我们采用的是数据datasource类型是HikariDataSource
     *  暂时只能用@Transactional，而这个注解默认采用的事务管理器是按照@Primay标注的数据源设定的，假设userDataSource是被@Primay标注的，则
     *  userJdbcTemplate的connection 是在事务管理中，出错可以回滚，而orderJdbcTemplate则会自动提交，出错无法回滚
     */
//    @Transactional(value = "orderDataSource", propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void createOrder(Order order) {
        userJdbcTemplate.update(SQL_UPDATE_DEPOSIT, order.getAmount(), order.getCustomerId());

        if (order.getTitle().contains("error1")) {
            throw new RuntimeException("Error1");
        }
        orderJdbcTemplate.update(SQL_CREATE_ORDER, order.getCustomerId(), order.getTitle(), order.getAmount());
        if (order.getTitle().contains("error2")) {
            throw new RuntimeException("Error2");
        }
    }

    public Map userInfo(Long customerId) {
        Map customer = userJdbcTemplate.queryForMap("select * from customer where id=" + customerId);
        List orders = orderJdbcTemplate.queryForList("select * from customer_order where customer_id=" + customerId);
        Map result = new HashMap();
        result.put("customer", customer);
        result.put("orders", orders);
        return result;
    }

}
