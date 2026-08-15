package reservation.binancebackend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GapCalculator {

    public Instant calculateStart(Optional<Instant> lastOpenTime, Instant now, int defaultLookbackDays) {
        return lastOpenTime
                .map(t -> t.plus(1, ChronoUnit.MINUTES))
                .orElse(now.minus(defaultLookbackDays, ChronoUnit.DAYS));
    }
}