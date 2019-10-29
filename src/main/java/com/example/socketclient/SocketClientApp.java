package com.example.socketclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.example.socketclient.SampleProducer.TOPIC;

@SpringBootApplication
@Slf4j
public class SocketClientApp {
    private final int NUM_CLIENTS = 1;
    private final int MAX_EVENTS = 25;


    //@Value("${app.client.url:http://stackoverflow-to-ws.default.35.224.5.101/questions/}")
    @Value("${app.client.url:http://localhost:8080/ws/feed}")
    private String uriString;

    private static String ENV_URI = System.getenv("WS_SERVER");
    private static boolean STORE_KAFKA = Boolean.parseBoolean(System.getenv("STORE_KAFKA"));

    URI getURI(String uri) {
        try {
            return new URI(ENV_URI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    Mono<Void> wsConnectNetty(SampleProducer producer) {
        URI uri = getURI(uriString);
        log.info("Connecting to URI:" + uri);
        return new ReactorNettyWebSocketClient().execute(uri,
                session -> session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .take(MAX_EVENTS)
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

    private void pipeToKafkaMessage(SampleProducer producer, WebSocketSession session, String txt) {
        try {
            if (STORE_KAFKA) {
                log.info("Writing message to kafka " + session.getId() + " -> " + txt);
                producer.sendMessages(TOPIC, txt);
                log.info(" Message written to kafka " + session.getId() + " -> " + txt);

            }
            else  {
                log.info(session.getId() + " -> " + txt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Bean
    ApplicationRunner appRunner() {

        log.info("appRunner creating producer with STORE_KAFKA: " + STORE_KAFKA);

        SampleProducer producer = null;
        if (STORE_KAFKA) {
            producer = new SampleProducer(SampleProducer.BOOTSTRAP_SERVERS);
        }

        SampleProducer finalProducer = producer;

        log.info("app runner producer created");

        return args -> {
            final CountDownLatch latch = new CountDownLatch(NUM_CLIENTS);

            ParallelFlux<Mono<Void>> parallelClients = Flux.range(0, NUM_CLIENTS)
                    .subscribeOn(Schedulers.single())
                    .map(n ->
                            connectToWS(finalProducer, latch)
                    )
                    .parallel();

            Flux.merge(
                    parallelClients
            )
                    .subscribe();

            latch.await(60, TimeUnit.SECONDS);
        };
    }

    private Mono<Void> connectToWS(SampleProducer producer, CountDownLatch latch) {
        Mono<Void> nettyMonoConnect = null;
        try {
            nettyMonoConnect = wsConnectNetty(producer)
                    .doOnTerminate(latch::countDown);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nettyMonoConnect;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(SocketClientApp.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
