package com.example.JFS_Job_Finding_Service.Controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/debug-db")
    public List<Object[]> debugDatabase() {
        System.out.println("🔍 Running native query...");
        Query query = entityManager.createNativeQuery("SELECT * FROM \"job\""); // Ensure correct table name
        List<Object[]> resultList = query.getResultList();

        System.out.println("✅ Query executed! Rows returned: " + resultList.size());
        for (Object[] row : resultList) {
            System.out.println("Row: " + java.util.Arrays.toString(row));
        }

        return resultList;
    }


}
