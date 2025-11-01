package com.example.JFS_Job_Finding_Service;

import org.springframework.boot.SpringApplication;

public class TestJfsJobFindingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(JFSApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
