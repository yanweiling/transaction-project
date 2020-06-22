package com.ywl.study.jms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author yanwlb
 * @Date 2020/6/8 11:10
 * 使用spring 原生的session管理jms
 */
@Service
public class CustomerService {
    private Logger log= LoggerFactory.getLogger(CustomerService.class);
    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Transactional
    @JmsListener(destination = "customer:msg1:new",containerFactory = "msgFactory")
    public void handler(String msg){
        log.info("Get msg1:{}",msg);
        String reply="Reply-"+msg;
        jmsTemplate.convertAndSend("customer:msg:reply",reply);//这一行的session默认是提交的
        if(msg.contains("error")){
            simulateError();
        }
    }

    private void simulateError() {
        throw new RuntimeException("some Data error.");
    }
    @JmsListener(destination = "customer:msg2:new", containerFactory = "msgFactory")
    public void handle2(String msg){
        log.debug("Get JMS message2 to from customer:{}", msg);
        DefaultTransactionDefinition def=new DefaultTransactionDefinition();
        TransactionStatus status=transactionManager.getTransaction(def);
        try {
            String reply = "Replied-2 - " + msg;
            jmsTemplate.convertAndSend("customer:msg:reply", reply);
            if (!msg.contains("error")) {
                transactionManager.commit(status);
            } else {
                transactionManager.rollback(status);
            }
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
