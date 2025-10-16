package com.example.IoT_UngDung.Config;

import io.swagger.v3.oas.annotations.headers.Header;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {
    private static final String BROKER = "tcp://192.168.1.57:1883";
    private static final String CLIENT_ID = "spring-boot-iot-server";
    private static final String USERNAME = "hello10x37";
    private static final String PASSWORD = "vangiap123";

    @Bean
    public MqttClient mqttClient() throws Exception {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        client.connect(options);
        return client;
    }




}
