package com.example.JFS_Job_Finding_Service.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

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
        options.setUserName(userName);
        options.setPassword(password.toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    // --- ĐỊNH NGHĨA CÁC CHANNELS (SPRING BEANS) ---

    @Bean
    public MessageChannel chatChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel presenceChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel requestChannel() {
        return new DirectChannel();
    }

    // --- ĐỊNH NGHĨA CÁC ADAPTERS (INBOUND) ---

    @Bean
    public MqttPahoMessageDrivenChannelAdapter chatAdapter(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("backend-chat-in", mqttClientFactory, "/chat/#");
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(chatChannel());
        return adapter;
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter presenceAdapter(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("backend-presence-in", mqttClientFactory, "/presence/#");
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(presenceChannel());
        return adapter;
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter requestAdapter(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("backend-request-in", mqttClientFactory, "/request/users/#");
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(requestChannel());
        return adapter;
    }
}