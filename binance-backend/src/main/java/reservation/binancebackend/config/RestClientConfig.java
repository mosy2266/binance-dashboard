package reservation.binancebackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient binanceApiRestClient(BinanceProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getRestBaseUrl())
                .build();
    }

    @Bean
    public ObjectMapper binanceWebSocketObjectMapper() {
        return new ObjectMapper();
    }
}