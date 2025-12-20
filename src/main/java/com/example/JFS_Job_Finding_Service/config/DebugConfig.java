package com.example.JFS_Job_Finding_Service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DebugConfig {
    @Autowired
    public DebugConfig(ApplicationContext ctx) {
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        System.out.println("🔍 Loaded Beans:");
        for (String bean : beanNames) {
            System.out.println("✅ " + bean);
        }
    }
}