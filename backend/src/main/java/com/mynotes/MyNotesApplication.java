package com.mynotes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.mynotes.config")
@MapperScan("com.mynotes.mapper")
public class MyNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyNotesApplication.class, args);
    }
}
