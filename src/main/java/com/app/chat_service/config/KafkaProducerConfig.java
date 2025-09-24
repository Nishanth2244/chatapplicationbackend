//package com.app.chat_service.config;
//
//import com.app.chat_service.model.ChatMessage;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaProducerConfig {
//
//    @Bean
//    public ProducerFactory<String, ChatMessage> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//
//        // ✅ Performance configs
//        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Leader ack only (fast)
//        configProps.put(ProducerConfig.LINGER_MS_CONFIG, "0"); // No delay, send instantly
//        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Small batches
//        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
//        configProps.put(ProducerConfig.RETRIES_CONFIG, 2); // retry a couple of times
//        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
//
//        // ✅ Allow large files up to 50MB
//        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 52428800);
//
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }
//
//    @Bean
//    public KafkaTemplate<String, ChatMessage> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//}
