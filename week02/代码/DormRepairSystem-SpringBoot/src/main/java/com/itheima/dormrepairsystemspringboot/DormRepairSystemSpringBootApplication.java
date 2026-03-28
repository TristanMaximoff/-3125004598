package com.itheima.dormrepairsystemspringboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.itheima.dormrepairsystemspringboot.mapper")
public class DormRepairSystemSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormRepairSystemSpringBootApplication.class, args);
    }

}
