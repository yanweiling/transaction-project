## 1.启动activemq

```
docker run --name='activemq' -d --rm -p 61616:61616 -p 8161:8161 webcenter/activemq:latest

```

## 2.登录控制台
http://101.201.125.82:8161/
默认登录账号 admin admin

## 3.新建项目spring-dtx-jms-db

### 3.1.pom文件

```
 <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-activemq</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```


### 3.2.配置文件

```
spring.datasource.url=jdbc:mysql://localhost:3306/user?useSSL=false&serverTimezone=PRC
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
#端口应该是61616
#spring.activemq.broker-url=tcp://101.201.125.82:32786
spring.activemq.broker-url=tcp://101.201.125.82:61616
```
### 3.3.dao

```
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
```

### 3.4.domain

```
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
    }
```
### 3.5.service

```
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

```

### 3.6.web

```
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

```
## 4.测试

```
curl -X POST  -d '{"username":"yanweiling","deposit":10}'  -H "Content-Type: application/json" http://localhost:8080/api/customer
```

登录数据库和mq控制台，我们发现mysql和mq数据都成功插入了

```
curl -X POST  -d '{"username":"yanweiling","deposit":10}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/msg?msg=testuser
```

登录数据库和mq控制台，我们发现mysql和mq数据都成功插入了


## 5.测试错误

```
curl -X POST  -d '{"username":"yanweiling","deposit":10}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/msg?msg=testuseerror2

```
我们会发现 数据库操作是事务管理中，但是jmsTemplate没有在事务管理中，发生异常也会自动提交

---
# 解决办法

新增配置类

```
package com.ywl.study.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

@Configuration
public class JmsConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://101.201.125.82:61616");
        TransactionAwareConnectionFactoryProxy proxy = new TransactionAwareConnectionFactoryProxy();
        proxy.setTargetConnectionFactory(cf);
        proxy.setSynchedLocalTransactionAllowed(true);//允许同步到LocalTransaction上面去
        return proxy;
    }

    /*如果我们不设置JmsTmeplate，有时候会有问题，有时候就没有问题*/
    /*如果不设置的话，有时候JmsTempalte就不会在事务中执行，就直接提交了，遇到异常也不会回滚*/
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setSessionTransacted(true);//让jmsTemplate发送消息是在事务中进行
        return jmsTemplate;

    }
}

```

再次执行
```
curl -X POST  -d '{"username":"yanweiling","deposit":10}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/msg?msg=testuseerror2

```

两个事务都回滚了，哈哈
```
2020-06-23 16:28:57.065 DEBUG 8756 --- [enerContainer-1] o.s.j.l.DefaultMessageListenerContainer  : Received message of type [class org.apache.activemq.command.ActiveMQTextMessage] from consumer [ActiveMQMessageConsumer { value=ID:yanwlb-58200-1592900700817-1:1:1:1, started=true }] of session [ActiveMQSession {id=ID:yanwlb-58200-1592900700817-1:1:1,started=true} java.lang.Object@6a41672d]
2020-06-23 16:28:57.066 DEBUG 8756 --- [enerContainer-1] .s.j.l.a.MessagingMessageListenerAdapter : Processing [org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener$MessagingMessageConverterAdapter$LazyResolutionMessage@6320ea4f]
2020-06-23 16:28:57.066 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [com.ywl.study.service.CustomerService.handler]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT; ''
2020-06-23 16:28:57.067 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [org.hibernate.jpa.internal.EntityManagerImpl@4d957f8d] for JPA transaction
2020-06-23 16:28:57.068 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC transaction [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@6c997a59]
2020-06-23 16:28:57.069  INFO 8756 --- [enerContainer-1] com.ywl.study.service.CustomerService    : Get msg:testuseerror2
2020-06-23 16:28:57.519 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [org.hibernate.jpa.internal.EntityManagerImpl@4d957f8d] for JPA transaction
2020-06-23 16:28:57.520 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Participating in existing transaction
2020-06-23 16:28:57.521 DEBUG 8756 --- [enerContainer-1] org.hibernate.SQL                        : insert into customer (deposit, username) values (?, ?)
2020-06-23 16:28:57.522 DEBUG 8756 --- [enerContainer-1] o.springframework.jms.core.JmsTemplate   : Executing callback on JMS Session: ActiveMQSession {id=ID:yanwlb-58200-1592900700817-1:1:1,started=true} java.lang.Object@6a41672d
2020-06-23 16:28:57.538 DEBUG 8756 --- [enerContainer-1] o.springframework.jms.core.JmsTemplate   : Sending created message: ActiveMQTextMessage {commandId = 0, responseRequired = false, messageId = null, originalDestination = null, originalTransactionId = null, producerId = null, destination = null, transactionId = null, expiration = 0, timestamp = 0, arrival = 0, brokerInTime = 0, brokerOutTime = 0, correlationId = null, replyTo = null, persistent = false, type = null, priority = 0, groupID = null, groupSequence = 0, targetConsumerId = null, compressed = false, userID = null, content = null, marshalledProperties = null, dataStructure = null, redeliveryCounter = 0, size = 0, properties = null, readOnlyProperties = false, readOnlyBody = false, droppable = false, jmsXGroupFirstForConsumer = false, text = testuseerror2}
2020-06-23 16:28:57.538 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
2020-06-23 16:28:57.538 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Rolling back JPA transaction on EntityManager [org.hibernate.jpa.internal.EntityManagerImpl@4d957f8d]
2020-06-23 16:28:57.656 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [org.hibernate.jpa.internal.EntityManagerImpl@4d957f8d] after transaction
2020-06-23 16:28:57.656 DEBUG 8756 --- [enerContainer-1] o.s.orm.jpa.EntityManagerFactoryUtils    : Closing JPA EntityManager
2020-06-23 16:28:57.657 DEBUG 8756 --- [enerContainer-1] o.s.j.l.DefaultMessageListenerContainer  : Initiating transaction rollback on application exception

```

