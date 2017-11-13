package com.cmh.config;

import javax.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("com.cmh.dao")
public class JpaConfiguration {

//    @Bean
//    public EntityManagerFactory entityManagerFactory() {
//        
//    }
}
