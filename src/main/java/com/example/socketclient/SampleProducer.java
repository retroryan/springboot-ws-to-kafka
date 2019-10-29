package com.example.socketclient;


import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sample producer application using Reactive API for Kafka.
 * To run sample producer
 * <ol>
 *   <li> Start Zookeeper and Kafka server
 *   <li> Update {@link #BOOTSTRAP_SERVERS} and {@link #TOPIC} if required
 *   <li> Create Kafka topic {@link #TOPIC}
 *   <li> Run {@link SampleProducer} as Java application with all dependent jars in the CLASSPATH (eg. from IDE).
 *   <li> Shutdown Kafka server and Zookeeper when no longer required
 * </ol>
 */
public class SampleProducer {

    private static final Logger log = LoggerFactory.getLogger(SampleProducer.class.getName());

    static String BOOTSTRAP_SERVERS = System.getenv("BOOTSTRAP_SERVER");
    static final String TOPIC = System.getenv("KAFKA_TOPIC");

    KafkaSender<Integer, String> sender;
    SimpleDateFormat dateFormat;

    public SampleProducer(String bootstrapServers) {

        log.info("Connecting to kafka in sample producer with boootstrap servers: " + bootstrapServers);
        log.info("TOPIC: " + TOPIC);

        try {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);

            log.info("kafka props: " + props);

            sender = KafkaSender.create(senderOptions);
            dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");

            log.info("KafkaSender created " + sender);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AtomicInteger messageKey = new AtomicInteger();
    public void sendMessages(String topic, String message) throws InterruptedException {
        sender.<Integer>send(Flux.range(1, 1)
                .map(i -> SenderRecord.create(new ProducerRecord<>(topic, messageKey.getAndIncrement(), message), i)))
                .doOnError(e -> log.error("Send failed", e))
                .subscribe(r -> {
                    RecordMetadata metadata = r.recordMetadata();
                    System.out.printf("Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(new Date(metadata.timestamp())));
                });
    }

    public void close() {
        sender.close();
    }

    public static void NOT_main(String[] args) throws Exception {
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        SampleProducer producer = new SampleProducer(BOOTSTRAP_SERVERS);
        //producer.sendMessages(TOPIC, count, latch);
        latch.await(10, TimeUnit.SECONDS);
        producer.close();
    }
}
