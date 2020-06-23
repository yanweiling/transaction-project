

## 新建项目spirng-dtx-jpa-db 
该项目复制与spring-dtx-db-db

**1.更改pom依赖**

```
 <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
```
改为

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
**
2.新建dao文件夹，新建接口CustomerRepository**

```
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
```

**3.customer 实体类更改**

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
    //get set....
    }
```
**4.注释掉db-db的链式事务**


```

//    /*创建链式事务管理器*/
//    @Bean
//    public PlatformTransactionManager transactionManager() {
//        /*spirng 会从容器中获取userDataSource和orderDataSource 注入到DataSourceTransactionManager构造函数中*/
//        /*如果是userTm.setDataSource(xxx) 的话，则不是从容器中获取datasource*/
//        DataSourceTransactionManager userTm = new DataSourceTransactionManager(userDataSource());
//        DataSourceTransactionManager orderTm = new DataSourceTransactionManager(orderDataSource());
//        ChainedTransactionManager chainManager = new ChainedTransactionManager(orderTm, userTm);
//        return chainManager;
//    }

```
**5.当发送请求**

```
curl -X POST  -d '{"customerId":1,"title":"testerror2","amount":2}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/order
```
时候，
user数据连接在jpa事务管理中，发生异常后回滚了，
但是orderJdbcTemplate 的操作不在jpa事务管理中，直接提交了


---
## 解决方案

在DBConfiguration创建jpa和datasource的链式事务


```
 @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        //不让自动创建表
        vendorAdapter.setGenerateDdl(false);
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setDataSource(userDataSource());
        factory.setPackagesToScan("com.ywl.study");
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager userTm = new JpaTransactionManager();
        userTm.setEntityManagerFactory(entityManagerFactory().getObject());
        PlatformTransactionManager orderTm = new DataSourceTransactionManager(orderDataSource());
        ChainedTransactionManager chainManager = new ChainedTransactionManager(orderTm, userTm);//提交顺序按照参数依次从后到前提交
        return chainManager;
    }

```
发送请求

```
curl -X POST  -d '{"customerId":1,"title":"testerror2","amount":2}'  -H "Content-Type: application/json" http://localhost:8080/api/customer/order
```
两个事务都回滚了

 日志
 
```
2020-06-23 11:49:06.010 DEBUG 8000 --- [nio-8080-exec-4] o.j.s.OpenEntityManagerInViewInterceptor : Opening JPA EntityManager in OpenEntityManagerInViewInterceptor
2020-06-23 11:49:06.012 DEBUG 8000 --- [nio-8080-exec-4] o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [com.ywl.study.service.CustomerService.createOrder]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2020-06-23 11:49:06.758 DEBUG 8000 --- [nio-8080-exec-4] o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [com.ywl.study.service.CustomerService.createOrder]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2020-06-23 11:49:06.764 DEBUG 8000 --- [nio-8080-exec-4] org.hibernate.SQL                        : select customer0_.id as id1_0_0_, customer0_.deposit as deposit2_0_0_, customer0_.username as username3_0_0_ from customer customer0_ where customer0_.id=?
2020-06-23 11:49:06.770 DEBUG 8000 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [insert into customer_order(customer_id,title,amount) values (?,?,?)]
2020-06-23 11:49:07.279 DEBUG 8000 --- [nio-8080-exec-4] o.s.orm.jpa.JpaTransactionManager        : Rolling back JPA transaction on EntityManager [SessionImpl(1873269348<open>)]
2020-06-23 11:49:07.284 DEBUG 8000 --- [nio-8080-exec-4] o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@1633693752 wrapping com.mysql.cj.jdbc.ConnectionImpl@7cbea31f]
2020-06-23 11:49:07.646 DEBUG 8000 --- [nio-8080-exec-4] o.j.s.OpenEntityManagerInViewInterceptor : Closing JPA EntityManager in OpenEntityManagerInViewInterceptor

```
