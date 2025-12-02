package com.example.JFS_Job_Finding_Service.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

@Configuration
public class MqttConfig {
    @Value("${spring.mqtt.url}")
    private String brokerUrl;
    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.username}")
    private String userName;
    @Value("${spring.mqtt.password}")
    private String password;
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(false);
        options.setUserName(userName); options.setPassword(password.toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }
}
