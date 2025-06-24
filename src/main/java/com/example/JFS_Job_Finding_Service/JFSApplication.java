package com.example.JFS_Job_Finding_Service;

import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.example.JFS_Job_Finding_Service")
@EnableJpaRepositories(basePackages = "com.example.JFS_Job_Finding_Service.repository")
@EntityScan(basePackages = "com.example.JFS_Job_Finding_Service.models")
@RestController
public class JFSApplication {
    public static void main(String[] args) {
        SpringApplication.run(JFSApplication.class, args);
    }
    @Bean
    CommandLineRunner testDatabase(DataSource dataSource) {
        return args -> {
            System.out.println("✅ Successfully connected to database: " + dataSource.getConnection().getMetaData().getURL());
        };
    }
    @Bean
    CommandLineRunner checkBeans(ApplicationContext ctx) {
        return args -> {
            String[] controllers = ctx.getBeanNamesForAnnotation(RestController.class);
            System.out.println("🔍 Found controllers: " + controllers.length);
            for (String controller : controllers) {
                System.out.println("✅ Controller loaded: " + controller);
            }
        };
    }
    @Bean
    CommandLineRunner checkRepositories(ApplicationContext ctx) {
        return args -> {
            String[] repos = ctx.getBeanNamesForAnnotation(Repository.class);
            System.out.println("🔍 Found repositories: " + repos.length);
            for (String repo : repos) {
                System.out.println("✅ Repository loaded: " + repo);
            }
        };
    }
    

}
