package com.ywl.study.jms.controller;

import com.ywl.study.jms.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author yanwlb
 * @Date 2020/6/8 11:18
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerResource {
    @Autowired
    JmsTemplate jmsTemplate;
    @Autowired
    CustomerService customerService;

    @PostMapping("/message1/listen")
    public void create(@RequestParam String msg){
        jmsTemplate.convertAndSend("customer:msg1:new",msg);
    }

    @PostMapping("/message1/direct")
    public void createMsg2irect(@RequestParam String msg) {
        customerService.handler(msg);
    }

    @GetMapping("/message")
    public String read(){
        jmsTemplate.setReceiveTimeout(2000);
        //如果不设置超时时间，则一直等待
        Object reply=jmsTemplate.receiveAndConvert("customer:msg:reply");
        return String.valueOf(reply);//如果是空的话，则返回"null"
    }

    @PostMapping("/message2/listen")
    public void createMsg2WithListener(@RequestParam String msg) {
        jmsTemplate.convertAndSend("customer:msg2:new", msg);
    }
    @PostMapping("/message2/direct")
    public void createMsg2Direct(@RequestParam String msg) {
        customerService.handle2(msg);
    }

}
