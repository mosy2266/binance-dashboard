package reservation.binancebackend.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MarketDataWebSocketHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarketDataWebSocketHandler handler = new MarketDataWebSocketHandler(objectMapper);

    @Test
    void broadcastsToConnectedSessionsOnly() throws Exception {
        WebSocketSession open = mock(WebSocketSession.class);
        when(open.isOpen()).thenReturn(true);
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(open);
        handler.afterConnectionEstablished(closed);

        handler.broadcast(java.util.Map.of("type", "ticker", "symbol", "BTCUSDT"));

        verify(open).sendMessage(new TextMessage(objectMapper.writeValueAsString(
                java.util.Map.of("type", "ticker", "symbol", "BTCUSDT"))));
        verify(closed, org.mockito.Mockito.never()).sendMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotBroadcastToSessionRemovedAfterDisconnect() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);
        handler.broadcast(java.util.Map.of("type", "ticker"));

        verify(session, org.mockito.Mockito.never()).sendMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNothingWhenNoSessionsConnected() {
        handler.broadcast(java.util.Map.of("type", "ticker"));
    }
}
