package com.patientsystem.apigateway;

import com.patientsystem.apigateway.filter.JwtClaimsFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtClaimsFilterTest {

    @InjectMocks
    JwtClaimsFilter filter;

    @Mock
    GatewayFilterChain chain;

    @Test
    void getOrder_returnsMinusOne_soFilterRunsFirst() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }

    @Test
    void filter_passesThrough_whenNoSecurityContext() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/patients").build()
        );

        try (MockedStatic<ReactiveSecurityContextHolder> contextHolder =
                     mockStatic(ReactiveSecurityContextHolder.class)) {

            contextHolder.when(ReactiveSecurityContextHolder::getContext)
                    .thenReturn(Mono.empty());

            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
        }
    }
}
