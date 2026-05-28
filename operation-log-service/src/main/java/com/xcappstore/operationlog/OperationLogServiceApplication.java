package com.xcappstore.operationlog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xcappstore.operationlog.mapper")
public class OperationLogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperationLogServiceApplication.class, args);
    }
}
