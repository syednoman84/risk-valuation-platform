package com.example.modelexecutionservice.external;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ExternalClientsConfig {

    private WebClient baseClient(String baseUrl) {
        // Netty client with timeouts
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10)));

        // Increase buffer if you expect larger JSON payloads
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    public WebClient positionWebClient(@Value("${services.position.base-url}") String baseUrl) {
        return baseClient(baseUrl);
    }

    @Bean
    public WebClient assumptionWebClient(@Value("${services.assumption.base-url}") String baseUrl) {
        return baseClient(baseUrl);
    }

    @Bean
    public WebClient modelWebClient(@Value("${services.model.base-url}") String baseUrl) {
        return baseClient(baseUrl);
    }
}
