package tech.nomad4.telegrambot4j.api;

import lombok.Getter;

import java.io.IOException;

/**
 * Thrown when Telegram responds with {@code error_code=429} (flood control). Carries the
 * {@code retry_after} value Telegram provides, so the caller knows exactly how long to wait
 * instead of guessing via exponential backoff.
 */
@Getter
public class TelegramRateLimitException extends IOException {

    private final int retryAfterSeconds;

    public TelegramRateLimitException(String description, int retryAfterSeconds) {
        super("Telegram API error: " + description);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
