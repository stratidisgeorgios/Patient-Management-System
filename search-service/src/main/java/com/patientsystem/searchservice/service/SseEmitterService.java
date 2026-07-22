package com.patientsystem.searchservice.service;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Service
public class SseEmitterService {
    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    public Flux<ServerSentEvent<String>> stream() {
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<String>builder().comment("heartbeat").build());

        return Flux.merge(
                sink.asFlux().map(type -> ServerSentEvent.<String>builder()
                        .event("indexUpdated")
                        .data(type)
                        .build()),
                heartbeat
        );
    }

    public void broadcast(String type) {
        sink.tryEmitNext(type);
    }
}
