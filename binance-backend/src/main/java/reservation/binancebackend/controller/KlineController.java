package reservation.binancebackend.controller;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reservation.binancebackend.dto.KlineDto;
import reservation.binancebackend.service.KlineQueryService;

@RestController
public class KlineController {

    private final KlineQueryService klineQueryService;

    public KlineController(KlineQueryService klineQueryService) {
        this.klineQueryService = klineQueryService;
    }

    @GetMapping("/api/klines")
    public List<KlineDto> getKlines(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return klineQueryService.getKlines(symbol, interval, from, to);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}