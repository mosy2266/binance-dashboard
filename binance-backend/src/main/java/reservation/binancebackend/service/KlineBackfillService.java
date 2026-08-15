package reservation.binancebackend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reservation.binancebackend.client.BinanceRestClient;
import reservation.binancebackend.config.BinanceProperties;
import reservation.binancebackend.entity.Kline;
import reservation.binancebackend.repository.KlineRepository;
import reservation.binancebackend.repository.KlineUpsertDao;

@Service
public class KlineBackfillService {

    private static final Logger log = LoggerFactory.getLogger(KlineBackfillService.class);
    private static final String INTERVAL = "1m";

    private final BinanceRestClient restClient;
    private final KlineRepository klineRepository;
    private final KlineUpsertDao upsertDao;
    private final KlineMapper mapper;
    private final GapCalculator gapCalculator;
    private final BinanceProperties properties;

    public KlineBackfillService(BinanceRestClient restClient, KlineRepository klineRepository,
            KlineUpsertDao upsertDao, KlineMapper mapper, GapCalculator gapCalculator,
            BinanceProperties properties) {
        this.restClient = restClient;
        this.klineRepository = klineRepository;
        this.upsertDao = upsertDao;
        this.mapper = mapper;
        this.gapCalculator = gapCalculator;
        this.properties = properties;
    }

    @Async("binanceTaskExecutor")
    public void backfillSymbol(String symbol) {
        BinanceProperties.Backfill cfg = properties.getBackfill();
        Instant now = Instant.now();
        Instant cursor = gapCalculator.calculateStart(
                klineRepository.findLastOpenTime(symbol), now, cfg.getDefaultLookbackDays());

        log.info("Backfill start symbol={} from={} to={}", symbol, cursor, now);

        while (cursor.isBefore(now)) {
            Instant pageEnd = cursor.plus(cfg.getPageLimit(), ChronoUnit.MINUTES);
            if (pageEnd.isAfter(now)) {
                pageEnd = now;
            }

            List<Kline> page = fetchPageWithRetry(symbol, cursor, pageEnd, cfg.getMaxRetries());
            if (page.isEmpty()) {
                break;
            }

            upsertDao.upsertBatch(page);
            cursor = page.get(page.size() - 1).getOpenTime().plus(1, ChronoUnit.MINUTES);

            sleep(cfg.getRequestDelayMs());
        }

        log.info("Backfill finished symbol={}", symbol);
    }

    private List<Kline> fetchPageWithRetry(String symbol, Instant start, Instant end, int maxRetries) {
        int attempt = 0;
        while (true) {
            try {
                List<List<Object>> rows = restClient.getKlines(symbol, INTERVAL, start, end, 1000);
                return rows.stream().map(row -> mapper.fromRestRow(symbol, row)).toList();
            } catch (RuntimeException e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw e;
                }
                log.warn("Backfill page fetch failed symbol={} attempt={} error={}", symbol, attempt, e.getMessage());
                sleep((long) Math.pow(2, attempt) * 1000);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}