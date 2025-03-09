package com.example.JFS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaRepositories("com.example.JFS.repository")
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class})
public class JFSApplication {
    public static void main(String[] args) {
        SpringApplication.run(JFSApplication.class, args);
    }
}
