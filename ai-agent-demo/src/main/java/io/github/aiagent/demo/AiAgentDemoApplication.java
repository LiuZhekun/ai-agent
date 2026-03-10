package io.github.aiagent.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Demo 启动类。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("io.github.aiagent.demo.mapper")
public class AiAgentDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentDemoApplication.class, args);
    }
}
