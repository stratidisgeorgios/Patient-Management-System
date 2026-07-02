package com.patientsystem.searchservice.service;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class SseEmitterService {
    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    public Flux<ServerSentEvent<String>> stream() {
        return sink.asFlux()
            .map(type -> ServerSentEvent.<String>builder()
                .event("indexUpdated")
                .data(type)
                .build());
    }

    public void broadcast(String type) {
        sink.tryEmitNext(type);
    }
}
