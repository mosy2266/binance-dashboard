package reservation.binancebackend.client;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceRestClient {

    private final RestClient restClient;

    public BinanceRestClient(RestClient binanceRestClient) {
        this.restClient = binanceRestClient;
    }

    @SuppressWarnings("unchecked")
    public List<List<Object>> getKlines(String symbol, String interval, Instant startTime, Instant endTime, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("startTime", startTime.toEpochMilli())
                        .queryParam("endTime", endTime.toEpochMilli())
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(List.class);
    }
}