package tech.nomad4.telegrambot4j.api;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.nomad4.telegrambot4j.model.Message;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link TelegramApiClient} honors Telegram's {@code retry_after} on a 429 response
 * instead of either giving up immediately (429 doesn't match {@code isRetryableError}'s
 * substring checks) or retrying on our own exponential-backoff schedule (which ignores what
 * Telegram actually asked for).
 */
class TelegramApiClientRateLimitTest {

    private MockWebServer server;
    private TelegramApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        TelegramApiConfig config = TelegramApiConfig.builder()
                .maxRetryAttempts(3)
                .build();
        client = new TelegramApiClient("test-token", config, server.url("/bot").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.shutdown();
    }

    @Test
    void sendMessage_rateLimited_waitsRetryAfterThenSucceeds() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {"ok": false, "error_code": 429, "description": "Too Many Requests: retry after 1",
                         "parameters": {"retry_after": 1}}
                        """));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {"ok": true, "result": {"message_id": 42, "chat": {"id": 1, "type": "private"}, "date": 1735689600, "text": "hi"}}
                        """));

        long start = System.currentTimeMillis();
        Message result = client.sendMessage(1L, "hi");
        long elapsedMillis = System.currentTimeMillis() - start;

        assertThat(result.getMessageId()).isEqualTo(42);
        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(1000);
    }

    @Test
    void sendMessage_rateLimitedPastMaxRetries_throwsRateLimitException() {
        for (int i = 0; i < 3; i++) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("""
                            {"ok": false, "error_code": 429, "description": "Too Many Requests: retry after 1",
                             "parameters": {"retry_after": 1}}
                            """));
        }

        assertThatThrownBy(() -> client.sendMessage(1L, "hi"))
                .isInstanceOf(TelegramRateLimitException.class);
    }

    @Test
    void sendMessage_nonRateLimitError_failsWithoutRetrying() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {"ok": false, "error_code": 400, "description": "Bad Request: chat not found"}
                        """));

        assertThatThrownBy(() -> client.sendMessage(1L, "hi"))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(TelegramRateLimitException.class)
                .hasMessageContaining("chat not found");

        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}
