package reservation.binancebackend.service;

import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IntervalParser {

    private static final Map<String, Duration> SUPPORTED = Map.of(
            "1m", Duration.ofMinutes(1),
            "5m", Duration.ofMinutes(5),
            "15m", Duration.ofMinutes(15),
            "1h", Duration.ofHours(1),
            "4h", Duration.ofHours(4),
            "1d", Duration.ofDays(1));

    public Duration parse(String interval) {
        Duration duration = SUPPORTED.get(interval);
        if (duration == null) {
            throw new IllegalArgumentException("Unsupported interval: " + interval);
        }
        return duration;
    }

    public boolean isBaseInterval(String interval) {
        return "1m".equals(interval);
    }
}