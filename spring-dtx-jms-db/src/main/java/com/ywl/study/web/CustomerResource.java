package com.ywl.study.web;

import com.ywl.study.dao.CustomerRepository;
import com.ywl.study.domain.Customer;
import com.ywl.study.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerResource {
    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    private CustomerService customerService;
    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("")
    public Customer createInAnnotation(@RequestBody Customer customer) {
        return customerService.create(customer);
    }

    @PostMapping("/msg")
    public void create(@RequestParam String msg) {
        jmsTemplate.convertAndSend("customer:msg:new", msg);
    }

    @GetMapping("")
    public List<Customer> getAll() {

        return customerRepository.findAll();
    }

    @GetMapping("/msg")
    public String getMsg() {
        jmsTemplate.setReceiveTimeout(3000);
        Object reply = jmsTemplate.receiveAndConvert("customer:msg:reply");
        return String.valueOf(reply);
    }
}
