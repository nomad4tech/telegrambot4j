package tech.nomad4.telegrambot4j.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.Duration;

/**
 * Configuration for TelegramApiClient (OkHttp-based)
 * Provides sensible defaults with ability to customize
 */
@Value
@Builder
@Getter
public class TelegramApiConfig {

    /**
     * Maximum idle connections in OkHttp ConnectionPool.
     * OkHttp will keep up to this many idle connections ready for reuse.
     */
    @Builder.Default
    int maxIdleConnections = 5;

    /**
     * Connection timeout - maximum time to establish a connection to the server
     */
    @Builder.Default
    Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout - maximum time to wait for response data after connection is established.
     * Maps to OkHttp readTimeout.
     */
    @Builder.Default
    Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Write timeout - maximum time to send request body.
     * Maps to OkHttp writeTimeout.
     */
    @Builder.Default
    Duration writeTimeout = Duration.ofSeconds(30);

    /**
     * Keep-alive duration for idle connections in OkHttp ConnectionPool.
     * Connections idle longer than this will be evicted.
     */
    @Builder.Default
    Duration keepAlive = Duration.ofSeconds(30);

    /**
     * Maximum retry attempts for transient errors
     */
    @Builder.Default
    int maxRetryAttempts = 3;

    /**
     * Initial backoff delay for retries
     */
    @Builder.Default
    Duration initialBackoff = Duration.ofMillis(500);

    /**
     * Maximum backoff delay for retries
     */
    @Builder.Default
    Duration maxBackoff = Duration.ofSeconds(5);

    /**
     * Backoff multiplier (exponential)
     */
    @Builder.Default
    double backoffMultiplier = 2.0;

    /**
     * Create default configuration
     */
    public static TelegramApiConfig defaults() {
        return TelegramApiConfig.builder().build();
    }
}
