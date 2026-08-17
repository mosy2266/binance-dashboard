package reservation.binancebackend.scheduler;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reservation.binancebackend.config.BinanceProperties;
import reservation.binancebackend.repository.KlineRepository;
import reservation.binancebackend.service.KlineBackfillService;

@Component
public class GapScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(GapScanScheduler.class);

    private final KlineRepository klineRepository;
    private final KlineBackfillService backfillService;
    private final BinanceProperties properties;

    public GapScanScheduler(KlineRepository klineRepository, KlineBackfillService backfillService,
            BinanceProperties properties) {
        this.klineRepository = klineRepository;
        this.backfillService = backfillService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@binanceProperties.gapScan.fixedDelayMinutes * 60000}")
    public void scan() {
        long thresholdMinutes = properties.getGapScan().getStalenessThresholdMinutes();
        Instant now = Instant.now();

        properties.getSymbols().forEach(symbol -> {
            Instant last = klineRepository.findLastOpenTime(symbol).orElse(null);
            if (last == null || Duration.between(last, now).toMinutes() > thresholdMinutes) {
                log.info("Gap detected symbol={} lastOpenTime={}, triggering backfill", symbol, last);
                backfillService.backfillSymbol(symbol, now);
            }
        });
    }
}