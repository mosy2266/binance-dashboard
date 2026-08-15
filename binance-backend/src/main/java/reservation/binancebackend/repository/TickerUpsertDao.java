package reservation.binancebackend.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import reservation.binancebackend.entity.Ticker24hStat;

@Repository
public class TickerUpsertDao {

    private static final String UPSERT_SQL = """
            INSERT INTO ticker_24h
                (symbol, last_price, price_change, price_change_percent,
                 high_price, low_price, volume, quote_volume, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (symbol) DO UPDATE SET
                last_price = EXCLUDED.last_price,
                price_change = EXCLUDED.price_change,
                price_change_percent = EXCLUDED.price_change_percent,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                volume = EXCLUDED.volume,
                quote_volume = EXCLUDED.quote_volume,
                updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public TickerUpsertDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(Ticker24hStat ticker) {
        jdbcTemplate.update(UPSERT_SQL,
                ticker.getSymbol(),
                ticker.getLastPrice(),
                ticker.getPriceChange(),
                ticker.getPriceChangePercent(),
                ticker.getHighPrice(),
                ticker.getLowPrice(),
                ticker.getVolume(),
                ticker.getQuoteVolume(),
                Timestamp.from(ticker.getUpdatedAt()));
    }
}