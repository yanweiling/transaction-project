package com.ywl.study.service;

import com.ywl.study.dao.CustomerRepository;
import com.ywl.study.domain.Customer;
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
    //    @Autowired
//    @Qualifier("userJdbcTemplate")
//    private JdbcTemplate userJdbcTemplate;
    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    @Qualifier("orderJdbcTemplate")
    private JdbcTemplate orderJdbcTemplate;

    //    private static final String SQL_UPDATE_DEPOSIT = "update customer set deposit=deposit-? where id=?";
    private static final String SQL_CREATE_ORDER = "insert into customer_order(customer_id,title,amount) values (?,?,?)";

    @Transactional
    public void createOrder(Order order) {
        Customer customer = customerRepository.getOne(order.getCustomerId());
        customer.setDeposit(customer.getDeposit() - order.getAmount());
        customerRepository.save(customer);
//        userJdbcTemplate.update(SQL_UPDATE_DEPOSIT, order.getAmount(), order.getCustomerId());

        if (order.getTitle().contains("error1")) {
            throw new RuntimeException("Error1");
        }
        orderJdbcTemplate.update(SQL_CREATE_ORDER, order.getCustomerId(), order.getTitle(), order.getAmount());
        if (order.getTitle().contains("error2")) {
            throw new RuntimeException("Error2");
        }
    }

    public Map userInfo(Long customerId) {
        Customer customer = customerRepository.getOne(customerId);
//        Map customer = userJdbcTemplate.queryForMap("select * from customer where id=" + customerId);
        List orders = orderJdbcTemplate.queryForList("select * from customer_order where customer_id=" + customerId);
        Map result = new HashMap();
        result.put("customer", customer);
        result.put("orders", orders);
        return result;
    }

}
