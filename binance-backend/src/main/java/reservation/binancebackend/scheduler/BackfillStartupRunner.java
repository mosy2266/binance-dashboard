package reservation.binancebackend.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reservation.binancebackend.config.BinanceProperties;
import reservation.binancebackend.service.KlineBackfillService;

@Component
public class BackfillStartupRunner {

    private final KlineBackfillService backfillService;
    private final BinanceProperties properties;

    public BackfillStartupRunner(KlineBackfillService backfillService, BinanceProperties properties) {
        this.backfillService = backfillService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        properties.getSymbols().forEach(backfillService::backfillSymbol);
    }
}