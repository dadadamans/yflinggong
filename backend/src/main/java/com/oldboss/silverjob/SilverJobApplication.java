package com.oldboss.silverjob;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.oldboss.silverjob.mapper")
public class SilverJobApplication {

    /**
     * 程序主入口。
     */
    public static void main(String[] args) {
        SpringApplication.run(SilverJobApplication.class, args);
    }
}
