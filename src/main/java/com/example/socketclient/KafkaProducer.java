package com.example.socketclient;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class KafkaProducer {

    static String BOOTSTRAP_SERVERS = System.getenv("BOOTSTRAP_SERVER");
    static final String TOPIC = System.getenv("KAFKA_TOPIC");

    KafkaSender<String, String> sender;
    SimpleDateFormat dateFormat;

    KafkaProducer(String bootstrapServers) {

        log.info("Connecting to kafka in sample producer with boootstrap servers: " + bootstrapServers);
        log.info("TOPIC: " + TOPIC);

        try {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            SenderOptions<String, String> senderOptions = SenderOptions.create(props);

            log.info("kafka props: " + props);

            sender = KafkaSender.create(senderOptions);
            dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");

            log.info("KafkaSender created " + sender);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AtomicInteger messageKey = new AtomicInteger();

    void sendMessages(String topic, String text) throws InterruptedException {
        sender.<Integer>send(Flux.just(SenderRecord.create(new ProducerRecord<>(topic, UUID.randomUUID().toString(), text), 1)))
                .doOnError(e -> log.error("Send failed", e))
                .subscribe(r -> {
                    RecordMetadata metadata = r.recordMetadata();
                    String msg = String.format("Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s text length=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(new Date(metadata.timestamp())),
                            text.length());
                    log.info(msg);
                });
    }

    public void close() {
        sender.close();
    }

}
