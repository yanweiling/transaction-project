package com.ywl.study.service;

import com.ywl.study.dao.CustomerRepository;
import com.ywl.study.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private static Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    JmsTemplate jmsTemplate;
    @Autowired
    CustomerRepository customerRepository;

    /*@JmsListener 标注的方法发生异常每次都要重试7次*/
    @Transactional
    @JmsListener(destination = "customer:msg:new")
    public void handler(String msg) {
        LOG.info("Get msg:{}", msg);
        Customer customer = new Customer();
        customer.setUsername(msg);
        customer.setDeposit(100);
        customerRepository.save(customer);
        if (msg.contains("error1")) {
            throw new RuntimeException("ERROR1");
        }
        jmsTemplate.convertAndSend("customer:msg:reply", msg);
        if (msg.contains("error2")) {
            throw new RuntimeException("ERROR2");
        }

    }

    @Transactional
    public Customer create(Customer customer) {
        LOG.info("create customer....");
        customerRepository.save(customer);
        if (customer.getUsername().contains("error1")) {
            throw new RuntimeException("ERROR1");
        }
        jmsTemplate.convertAndSend("customer:msg:reply", customer.getUsername() + " created");
        if (customer.getUsername().contains("error2")) {
            throw new RuntimeException("ERROR2");
        }
        return customer;

    }
}
