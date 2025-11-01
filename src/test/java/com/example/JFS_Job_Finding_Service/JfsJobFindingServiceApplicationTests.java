package com.example.JFS_Job_Finding_Service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JfsJobFindingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
