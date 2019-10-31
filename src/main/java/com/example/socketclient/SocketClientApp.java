package com.example.socketclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static com.example.socketclient.KafkaProducer.TOPIC;

@SpringBootApplication
@Slf4j
public class SocketClientApp {
    private final int NUM_CLIENTS = 1;

    private static String ENV_URI = System.getenv("WS_SERVER");
    private static boolean STORE_KAFKA = Boolean.parseBoolean(System.getenv("STORE_KAFKA"));

    URI getURI() {
        try {
            return new URI(ENV_URI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    Mono<Void> wsConnectNetty(KafkaProducer producer) {
        URI uri = getURI();
        log.info("Connecting to URI:" + uri);
        return new ReactorNettyWebSocketClient().execute(uri,
                session -> session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(txt -> {
                            pipeToKafkaMessage(producer, session, txt);
                        })
                        .doOnSubscribe(subscriber -> log.info(session.getId() + ".OPEN"))
                        .doFinally(signalType -> {
                            session.close();
                            log.info(session.getId() + ".CLOSE");
                        })
                        .then()

        );
    }

    private void pipeToKafkaMessage(KafkaProducer producer, WebSocketSession session, String txt) {
        try {
            if (STORE_KAFKA) {
                producer.sendMessages(TOPIC, txt);
                log.debug(" writing text to kafka " + session.getId() + " -> " + txt + " \n");

            } else {
                log.info(session.getId() + " -> " + txt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    ApplicationRunner appRunner() {
        log.info("appRunner creating producer with STORE_KAFKA: " + STORE_KAFKA);
        KafkaProducer producer = null;
        if (STORE_KAFKA) {
            producer = new KafkaProducer(KafkaProducer.BOOTSTRAP_SERVERS);
        }
        KafkaProducer finalProducer = producer;
        return args -> {
            connectToWS(finalProducer).subscribe();
        };
    }

    private Mono<Void> connectToWS(KafkaProducer producer) {
        Mono<Void> nettyMonoConnect = null;
        try {
            nettyMonoConnect = wsConnectNetty(producer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nettyMonoConnect;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(SocketClientApp.class);
        app.run(args);
    }
}
