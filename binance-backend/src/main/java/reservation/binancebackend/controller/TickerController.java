package reservation.binancebackend.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reservation.binancebackend.dto.TickerDto;
import reservation.binancebackend.entity.Ticker24hStat;
import reservation.binancebackend.repository.TickerRepository;

@RestController
public class TickerController {

    private final TickerRepository tickerRepository;

    public TickerController(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
    }

    @GetMapping("/api/tickers")
    public List<TickerDto> getTickers() {
        return tickerRepository.findAll().stream().map(this::toDto).toList();
    }

    private TickerDto toDto(Ticker24hStat t) {
        return new TickerDto(t.getSymbol(), t.getLastPrice(), t.getPriceChange(),
                t.getPriceChangePercent(), t.getHighPrice(), t.getLowPrice(),
                t.getVolume(), t.getQuoteVolume(), t.getUpdatedAt());
    }
}