//package com.app.chat_service.config;
//
//import com.app.chat_service.model.ChatMessage;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaConsumerConfig {
//
//    @Bean
//    public ConsumerFactory<String, ChatMessage> consumerFactory() {
//        JsonDeserializer<ChatMessage> deserializer = new JsonDeserializer<>(ChatMessage.class);
//        deserializer.addTrustedPackages("*");
//        deserializer.setRemoveTypeHeaders(false);
//        deserializer.setUseTypeMapperForKey(true);
//
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-group");
//
//        // ✅ Deserialization
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
//
//        // ✅ Performance configs
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // only new messages
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); // process quickly, not big batches
//        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
//
//        // ✅ Large message support
//        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 52428800); // 50MB per partition
//        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800); // 50MB total
//
//        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, ChatMessage> chatKafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, ChatMessage> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory());
//
//        // ✅ Low latency parallel consumers
//        factory.setConcurrency(3);
//        factory.setBatchListener(false);
//
//        return factory;
//    }
//}
