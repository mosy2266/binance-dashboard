package reservation.binancebackend.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "binance")
@Getter
@Setter
public class BinanceProperties {

    private List<String> symbols;
    private String restBaseUrl;
    private String wsBaseUrl;
    private Backfill backfill = new Backfill();
    private GapScan gapScan = new GapScan();

    @Getter
    @Setter
    public static class Backfill {
        private int defaultLookbackDays;
        private long requestDelayMs;
        private int pageLimit;
        private int maxRetries;
    }

    @Getter
    @Setter
    public static class GapScan {
        private long fixedDelayMinutes;
        private long stalenessThresholdMinutes;
    }
}