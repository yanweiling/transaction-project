package com.ywl.study.jms.config;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;

/**
 * @author yanwlb
 * @Date 2020/6/8 14:03
 */
@EnableJms
@Configuration
public class JmsConfig {
    //jms transactionmanager 实际上还是用的session进行管理的
    //而session是根据connectionFactory获取的
    @Bean
    PlatformTransactionManager transactionManager( ConnectionFactory cf){
        return new JmsTransactionManager(cf);
    }

    @Bean
    JmsTemplate jmsTemplate(ConnectionFactory cf){
        JmsTemplate jmsTemplate= new JmsTemplate();
        jmsTemplate.setConnectionFactory(cf);
        return jmsTemplate;
    }

    @Bean
    public JmsListenerContainerFactory<?> msgFactory(ConnectionFactory cf,
                                                     DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                     PlatformTransactionManager transactionManager) {
        //factory 的设置会影响jmsListener监听读消息的配置
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setTransactionManager(transactionManager);
        factory.setReceiveTimeout(30000l);
        //containerFactory 同一个containerFactory监听不同的队列时，其中一个队列的监听session关闭时，会把connection也彻底关闭掉；
        //这样导致监听另一个队列的session也被迫关闭掉
        factory.setCacheLevelName("CACHE_CONNECTION");//当session被关闭的时候，可以不是直接close掉connection，而是将connection被缓存起来
        configurer.configure(factory, cf);
        return factory;
    }
}
