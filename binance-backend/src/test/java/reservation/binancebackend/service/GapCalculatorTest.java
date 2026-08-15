package reservation.binancebackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GapCalculatorTest {

    private final GapCalculator gapCalculator = new GapCalculator();

    @Test
    void calculateStart_usesLastOpenTimePlusOneMinute_whenDataExists() {
        Instant lastOpenTime = Instant.parse("2026-08-01T00:00:00Z");
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        Instant start = gapCalculator.calculateStart(Optional.of(lastOpenTime), now, 90);

        assertThat(start).isEqualTo(lastOpenTime.plus(1, ChronoUnit.MINUTES));
    }

    @Test
    void calculateStart_usesDefaultLookback_whenNoDataExists() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        Instant start = gapCalculator.calculateStart(Optional.empty(), now, 90);

        assertThat(start).isEqualTo(now.minus(90, ChronoUnit.DAYS));
    }
}