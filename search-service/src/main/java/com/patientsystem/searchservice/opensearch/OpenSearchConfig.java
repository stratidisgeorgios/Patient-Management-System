package com.patientsystem.searchservice.opensearch;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    private String host;

    @Value("${opensearch.port}")
    private int port;

    @Bean
    public OpenSearchClient openSearchClient() {
        HttpHost httpHost = new HttpHost("http", host, port);
        var transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost[]{httpHost}).build();
        return new OpenSearchClient(transport);
    }
}
