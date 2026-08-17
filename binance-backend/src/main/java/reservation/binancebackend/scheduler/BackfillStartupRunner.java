package reservation.binancebackend.scheduler;

import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reservation.binancebackend.client.BinanceWebSocketClient;
import reservation.binancebackend.config.BinanceProperties;
import reservation.binancebackend.service.KlineBackfillService;

@Component
public class BackfillStartupRunner {

    private final KlineBackfillService backfillService;
    private final BinanceProperties properties;
    private final BinanceWebSocketClient webSocketClient;

    public BackfillStartupRunner(KlineBackfillService backfillService, BinanceProperties properties,
            BinanceWebSocketClient webSocketClient) {
        this.backfillService = backfillService;
        this.properties = properties;
        this.webSocketClient = webSocketClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Instant now = Instant.now();
        properties.getSymbols().forEach(symbol -> backfillService.backfillSymbol(symbol, now));
        webSocketClient.connect();
    }
}