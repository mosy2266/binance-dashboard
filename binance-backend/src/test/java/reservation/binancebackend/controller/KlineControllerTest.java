package reservation.binancebackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reservation.binancebackend.dto.KlineDto;
import reservation.binancebackend.service.KlineQueryService;

@WebMvcTest(KlineController.class)
class KlineControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private KlineQueryService klineQueryService;

    @Test
    void getKlines_returnsOk_forValidRequest() {
        when(klineQueryService.getKlines(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(new KlineDto(Instant.now(), null, null, null, null, null, null, 0)));

        mvc.get()
                .uri("/api/klines?symbol=BTCUSDT&interval=1m&from=2026-08-15T00:00:00Z&to=2026-08-15T01:00:00Z")
                .assertThat()
                .hasStatusOk();
    }

    @Test
    void getKlines_returnsBadRequest_forUnsupportedInterval() {
        when(klineQueryService.getKlines(anyString(), anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("Unsupported interval: 2m"));

        mvc.get()
                .uri("/api/klines?symbol=BTCUSDT&interval=2m&from=2026-08-15T00:00:00Z&to=2026-08-15T01:00:00Z")
                .assertThat()
                .hasStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}