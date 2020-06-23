# 1.演示多个数据源，事务无法一起回滚

### 1.pom

```
 <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>2.7.8</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <!--<version>5.1.39</version>-->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```
### 2.domain 类

2.1.Customer--存在于user数据库

```
public class Customer {

    private Long id;

    private String username;

    /*余额*/
    private Integer deposit;
    //get set....
    }
```

2.2.Order--存在于order数据库

```
public class Order {
    private Long id;
    private Long customerId;
    private String title;
    private Integer amount;
    //get set ...
    }
```

### 3.application.properties

```
spring.ds.user.url=jdbc:mysql://localhost:3306/user?useSSL=false&serverTimezone=PRC
spring.ds.user.username=root
spring.ds.user.password=123456
spring.ds.user.driver-class-name=com.mysql.cj.jdbc.Driver
spring.ds.order.url=jdbc:mysql://101.201.125.82:3306/order?useSSL=false&serverTimezone=PRC
spring.ds.order.username=root
spring.ds.order.password=123456
spring.ds.order.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 4.SQL脚本

user数据库

```
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for customer
-- ----------------------------
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `deposit` int(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customer
-- ----------------------------
INSERT INTO `customer` VALUES (1, 'zhangsan', 100);

SET FOREIGN_KEY_CHECKS = 1;
```

order数据库

```
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for customer_order
-- ----------------------------
DROP TABLE IF EXISTS `customer_order`;
CREATE TABLE `customer_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `customer_id` bigint(20) NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `amount` int(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
```

### 5.数据源配置类-DBConfiguration

```


@Configuration
public class DBConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.ds.user")
    public DataSourceProperties userDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource userDataSource() {
        return userDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public JdbcTemplate userJdbcTemplate(@Qualifier("userDataSource") DataSource userDataSource) {
        return new JdbcTemplate(userDataSource);
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.ds.order")
    public DataSourceProperties orderDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource orderDataSource() {
        return orderDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public JdbcTemplate orderJdbcTemplate(@Qualifier("orderDataSource") DataSource orderDataSource) {
        return new JdbcTemplate(orderDataSource);
    }

}

```

### 6.service

```
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
        orderJdbcTemplate.update(SQL_CREATE_ORDER, order.getCustomerId(), order.getTitle(), order.getAmount());

        if (order.getTitle().contains("error1")) {
            throw new RuntimeException("Error1");
        }
        userJdbcTemplate.update(SQL_UPDATE_DEPOSIT, order.getAmount(), order.getCustomerId());
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
```
### 7.web服务

```
@RestController
@RequestMapping("/api/customer")
public class CustomerResource {
    @Autowired
    CustomerService customerService;

    @PostMapping("/order")
    public void create(@RequestBody Order order){
        customerService.createOrder(order);
    }

    @GetMapping("/{id}")
    public Map userInfo(@PathVariable Long id){
       return customerService.userInfo(id);
    }
}
```

### 8.日志

```
<?xml version="1.0" encoding="UTF-8"?>

<configuration scan="true">
    <include resource="org/springframework/boot/logging/logback/base.xml"/>

    <!-- The FILE and ASYNC appenders are here as examples for a production configuration -->
    <logger name="com.ywl.study" level="DEBUG"/>
    <logger name="org.springframework.transaction" level="DEBUG"/>
    <logger name="org.springframework.jms" level="DEBUG"/>
    <logger name="org.springframework.jdbc" level="DEBUG"/>
    <logger name="org.springframework.orm.jpa" level="DEBUG"/>
    <logger name="javax.transaction" level="DEBUG"/>
    <logger name="javax.jms" level="DEBUG"/>
    <logger name="org.hibernate.jpa" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```
### 9.演示


```
curl -X POST  -d '{"customerId":1,"title":"testerror2","amount":2}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/order
```
我们会发现user库中的customer回滚了，但是order库中的customer_order直接提交了


---
# 链式事务，解决方案

1.pom依赖

```
 <!--链式事务管理器依赖包-->
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-commons</artifactId>
        </dependency>
        <!--链式事务管理器依赖包-->
```
2.在DBConfiguration中创建链式事务管理器

```
 /*创建链式事务管理器*/
    @Bean
    public PlatformTransactionManager transactionManager() {
        /*spirng 会从容器中获取userDataSource和orderDataSource 注入到DataSourceTransactionManager构造函数中*/
        /*如果是userTm.setDataSource(xxx) 的话，则不是从容器中获取datasource*/
        DataSourceTransactionManager userTm = new DataSourceTransactionManager(userDataSource());
        DataSourceTransactionManager orderTm = new DataSourceTransactionManager(orderDataSource());
        ChainedTransactionManager chainManager = new ChainedTransactionManager(orderTm,userTm);
        return chainManager;
    }
```
事务执行顺序

    创建orderTm事务管理器
    创建userTm事务管理器
    提交userTm事务管理器
    提交orderTm事务管理器

3.发送请求

```
curl -X POST  -d '{"customerId":1,"title":"testerror2","amount":2}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/order
```


```
2020-06-23 10:19:19.456 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [com.ywl.study.service.CustomerService.createOrder]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2020-06-23 10:19:19.457 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@1670176962 wrapping com.mysql.cj.jdbc.ConnectionImpl@62723a89] for JDBC transaction
2020-06-23 10:19:19.458 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@1670176962 wrapping com.mysql.cj.jdbc.ConnectionImpl@62723a89] to manual commit
2020-06-23 10:19:19.458 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [com.ywl.study.service.CustomerService.createOrder]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2020-06-23 10:19:19.477 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@1833636547 wrapping com.mysql.cj.jdbc.ConnectionImpl@4085a243] for JDBC transaction
2020-06-23 10:19:19.477 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@1833636547 wrapping com.mysql.cj.jdbc.ConnectionImpl@4085a243] to manual commit
2020-06-23 10:19:19.548 DEBUG 27312 --- [nio-8080-exec-5] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL update
2020-06-23 10:19:19.549 DEBUG 27312 --- [nio-8080-exec-5] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [update customer set deposit=deposit-? where id=?]
2020-06-23 10:19:19.550 DEBUG 27312 --- [nio-8080-exec-5] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL update
2020-06-23 10:19:19.550 DEBUG 27312 --- [nio-8080-exec-5] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [insert into customer_order(customer_id,title,amount) values (?,?,?)]
2020-06-23 10:19:19.601 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
2020-06-23 10:19:19.602 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@1833636547 wrapping com.mysql.cj.jdbc.ConnectionImpl@4085a243]
2020-06-23 10:19:19.636 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@1833636547 wrapping com.mysql.cj.jdbc.ConnectionImpl@4085a243] after transaction
2020-06-23 10:19:19.637 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Resuming suspended transaction after completion of inner transaction
2020-06-23 10:19:19.637 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
2020-06-23 10:19:19.637 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@1670176962 wrapping com.mysql.cj.jdbc.ConnectionImpl@62723a89]
2020-06-23 10:19:19.670 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@1670176962 wrapping com.mysql.cj.jdbc.ConnectionImpl@62723a89] after transaction
2020-06-23 10:19:19.670 DEBUG 27312 --- [nio-8080-exec-5] o.s.j.d.DataSourceTransactionManager     : Resuming suspended transaction after completion of inner transaction
2020-06-23 10:19:19.683 ERROR 27312 --- [nio-8080-exec-5] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is java.lang.RuntimeException: Error2] with root cause

java.lang.RuntimeException: Error2
	at com.ywl.study.service.CustomerService.createOrder(CustomerService.java:47)
```
我们发现两个库的事务都回滚成功了


4.模拟当其中一个数据连接提交后，另一个数据库宕机的情况

在DataSourceTransactionManager类的

```
    protected void doCommit(DefaultTransactionStatus status) {
        DataSourceTransactionManager.DataSourceTransactionObject txObject = (DataSourceTransactionManager.DataSourceTransactionObject)status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        if (status.isDebug()) {
            this.logger.debug("Committing JDBC transaction on Connection [" + con + "]");
        }

        try {
            con.commit();
        } catch (SQLException var5) {
            throw new TransactionSystemException("Could not commit JDBC transaction", var5);
        }
    }
```

的con.commit()上打断点

当第一个事务提交以后，
停掉第二个数据的服务

    root@iZ2ze3etdjb8e6eifs9xylZ:~# docker stop d9dc1fd29235
    d9dc1fd29235


然后执行程序到结束，然后重启第二个数据库服务

    docker start d9dc1fd29235


我们会发现第一个数据库user中的表已经减少了余额；
但是第二个数据库中的订单表却没有生成







