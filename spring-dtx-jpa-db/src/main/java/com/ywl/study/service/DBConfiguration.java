package com.ywl.study.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

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

}
