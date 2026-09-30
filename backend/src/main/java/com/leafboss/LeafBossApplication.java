package com.leafboss;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.leafboss.mapper")
public class LeafBossApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeafBossApplication.class, args);
    }

}
