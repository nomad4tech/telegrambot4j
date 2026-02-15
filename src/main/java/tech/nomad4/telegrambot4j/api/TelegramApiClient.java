package tech.nomad4.telegrambot4j.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import tech.nomad4.telegrambot4j.model.Message;
import tech.nomad4.telegrambot4j.model.Update;
import tech.nomad4.telegrambot4j.model.User;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Telegram Bot API Client - production-ready POJO client with connection pooling and retry logic.
 * Can work both standalone and in Spring context.
 *
 * <p><b>Long polling and readTimeout:</b><br>
 * OkHttp's readTimeout must always be greater than the Telegram long polling timeout.
 * If readTimeout <= pollingTimeout, OkHttp will terminate the connection before Telegram
 * responds, causing the polling loop to stop unexpectedly.
 *
 * <p>This client handles this automatically in {@link #getUpdates}: for each call, if the
 * configured readTimeout is insufficient, a temporary OkHttpClient is created via
 * {@code httpClient.newBuilder()} with readTimeout dynamically set to
 * {@code pollingTimeout + POLLING_READ_TIMEOUT_BUFFER_SECONDS}. The builder reuses the same
 * connection pool and settings - only the readTimeout is overridden. If user configured
 * readTimeout is already sufficient, the base client is used as-is.
 */
@Slf4j
public class TelegramApiClient implements Closeable {

    private static final String API_URL = "https://api.telegram.org/bot";
    private static final ObjectMapper SHARED_OBJECT_MAPPER = createObjectMapper();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json");

    /**
     * Extra buffer added on top of Telegram's pollingTimeout for OkHttp readTimeout.
     * Ensures OkHttp never cuts the connection before Telegram finishes responding.
     */
    private static final int POLLING_READ_TIMEOUT_BUFFER_SECONDS = 35;

    private final String botToken;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TelegramApiConfig config;
    private final boolean ownsHttpClient;

    /**
     * Constructor with default configuration
     */
    public TelegramApiClient(String botToken) {
        this(botToken, TelegramApiConfig.defaults());
    }

    /**
     * Constructor with custom configuration
     */
    public TelegramApiClient(String botToken, TelegramApiConfig config) {
        this.botToken = botToken;
        this.config = config;
        this.httpClient = createHttpClient(config);
        this.objectMapper = SHARED_OBJECT_MAPPER;
        this.ownsHttpClient = true;
    }

    /**
     * Constructor with custom OkHttpClient.
     * For maximum flexibility - user provides pre-configured client.
     * Note: readTimeout will be dynamically adjusted per-request in getUpdates if needed.
     */
    public TelegramApiClient(String botToken, OkHttpClient httpClient) {
        this.botToken = botToken;
        this.config = TelegramApiConfig.defaults();
        this.httpClient = httpClient;
        this.objectMapper = SHARED_OBJECT_MAPPER;
        this.ownsHttpClient = false;
    }

    /**
     * Constructor with custom OkHttpClient and ObjectMapper
     */
    public TelegramApiClient(String botToken, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.botToken = botToken;
        this.config = TelegramApiConfig.defaults();
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.ownsHttpClient = false;
    }

    /**
     * Create shared ObjectMapper with global settings
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Create OkHttpClient with connection pooling and timeouts from config
     */
    private static OkHttpClient createHttpClient(TelegramApiConfig config) {
        ConnectionPool connectionPool = new ConnectionPool(
                config.getMaxIdleConnections(),
                config.getKeepAlive().toMillis(),
                TimeUnit.MILLISECONDS
        );

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .connectionPool(connectionPool)
                .build();

        log.info("Created OkHttpClient: maxIdleConnections={}, keepAlive={}ms",
                config.getMaxIdleConnections(), config.getKeepAlive().toMillis());

        return client;
    }

    /**
     * Get updates using long polling.
     * <a href="https://core.telegram.org/bots/api#getupdates">Telegram API docs</a>
     *
     * <p><b>NO RETRY</b> - long polling requests should not be retried.
     *
     * <p><b>Dynamic readTimeout:</b> If the configured readTimeout is insufficient for the
     * requested polling timeout, a temporary OkHttpClient is built via
     * {@code httpClient.newBuilder()} with readTimeout set to
     * {@code timeout + POLLING_READ_TIMEOUT_BUFFER_SECONDS}. This guarantees OkHttp never
     * cuts the connection before Telegram responds. The builder shares the same ConnectionPool
     * as the base client - no extra resources used. If configured readTimeout is already
     * sufficient, the base client is used as-is.
     */
    public List<Update> getUpdates(Long offset, Integer limit, Integer timeout) throws IOException {
        Map<String, Object> params = new HashMap<>();
        if (offset != null) params.put("offset", offset);
        if (limit != null) params.put("limit", limit);
        if (timeout != null) params.put("timeout", timeout);

        // Calculate required readTimeout for this polling request
        int pollingTimeout = timeout != null ? timeout : 30;
        int requiredReadTimeout = pollingTimeout + POLLING_READ_TIMEOUT_BUFFER_SECONDS;

        // Check if we need to adjust readTimeout
        // For custom OkHttpClient (ownsHttpClient=false), we can't reliably know the configured
        // readTimeout, so we always create a new client to be safe
        OkHttpClient clientToUse;
        if (ownsHttpClient && config.getReadTimeout().toSeconds() >= requiredReadTimeout) {
            // User's configured readTimeout is sufficient, use base client
            clientToUse = httpClient;
        } else {
            // Build one-off client with sufficient readTimeout
            // OkHttpClient.newBuilder() is lightweight - reuses the same ConnectionPool,
            // dispatcher, and all other settings. Only readTimeout is overridden.
            clientToUse = httpClient.newBuilder()
                    .readTimeout(requiredReadTimeout, TimeUnit.SECONDS)
                    .build();
        }

        TelegramResponse<List<Update>> response = executeMethodInternal(
                clientToUse,
                "getUpdates",
                params,
                new TypeReference<TelegramResponse<List<Update>>>() {}
        );

        if (response.getOk()) {
            return response.getResult();
        } else {
            throw new IOException("Telegram API error: " + response.getDescription());
        }
    }

    /**
     * Send text message
     * <a href="https://core.telegram.org/bots/api#sendmessage">...</a>
     */
    public Message sendMessage(Long chatId, String text) throws IOException {
        return sendMessage(chatId, text, null, null, null);
    }

    /**
     * Send text message with additional parameters
     */
    public Message sendMessage(Long chatId, String text, String parseMode,
                               Integer replyToMessageId, Object replyMarkup) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("chat_id", chatId);
        params.put("text", text);
        if (parseMode != null) params.put("parse_mode", parseMode);
        if (replyToMessageId != null) params.put("reply_to_message_id", replyToMessageId);
        if (replyMarkup != null) params.put("reply_markup", replyMarkup);

        TelegramResponse<Message> response = executeMethodWithRetry(
                "sendMessage",
                params,
                new TypeReference<TelegramResponse<Message>>() {}
        );

        if (response.getOk()) {
            return response.getResult();
        } else {
            throw new IOException("Telegram API error: " + response.getDescription());
        }
    }

    /**
     * Answer callback query
     * <a href="https://core.telegram.org/bots/api#answercallbackquery">...</a>
     */
    public Boolean answerCallbackQuery(String callbackQueryId, String text, Boolean showAlert) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("callback_query_id", callbackQueryId);
        if (text != null) params.put("text", text);
        if (showAlert != null) params.put("show_alert", showAlert);

        TelegramResponse<Boolean> response = executeMethodWithRetry(
                "answerCallbackQuery",
                params,
                new TypeReference<TelegramResponse<Boolean>>() {}
        );

        if (response.getOk()) {
            return response.getResult();
        } else {
            throw new IOException("Telegram API error: " + response.getDescription());
        }
    }

    /**
     * Get bot info
     * <a href="https://core.telegram.org/bots/api#getme">...</a>
     */
    public User getMe() throws IOException {
        TelegramResponse<User> response = executeMethodWithRetry(
                "getMe",
                new HashMap<>(),
                new TypeReference<TelegramResponse<User>>() {}
        );

        if (response.getOk()) {
            return response.getResult();
        } else {
            throw new IOException("Telegram API error: " + response.getDescription());
        }
    }

    /**
     * Execute any Telegram API method with retry logic
     */
    public <T> T executeMethod(String method, Map<String, Object> params, Class<T> responseType) throws IOException {
        return executeMethodWithRetry(method, params, response ->
                objectMapper.readValue(response, responseType));
    }

    /**
     * Execute any Telegram API method with TypeReference and retry logic
     */
    public <T> T executeMethod(String method, Map<String, Object> params, TypeReference<T> typeReference) throws IOException {
        return executeMethodWithRetry(method, params, response ->
                objectMapper.readValue(response, typeReference));
    }

    /**
     * Execute method with retry logic for transient errors
     */
    private <T> T executeMethodWithRetry(String method, Map<String, Object> params,
                                         ResponseParser<T> parser) throws IOException {
        int attempt = 0;
        long backoffMillis = config.getInitialBackoff().toMillis();

        while (true) {
            try {
                String response = executeHttpRequest(httpClient, method, params);
                return parser.parse(response);
            } catch (IOException e) {
                attempt++;

                if (attempt >= config.getMaxRetryAttempts()) {
                    log.error("Max retry attempts ({}) reached for method {}", config.getMaxRetryAttempts(), method);
                    throw e;
                }

                if (!isRetryableError(e)) {
                    log.debug("Non-retryable error for method {}: {}", method, e.getMessage());
                    throw e;
                }

                log.warn("Retryable error on attempt {} for method {}: {}. Retrying in {}ms",
                        attempt, method, e.getMessage(), backoffMillis);

                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry backoff", ie);
                }

                backoffMillis = Math.min(
                        (long) (backoffMillis * config.getBackoffMultiplier()),
                        config.getMaxBackoff().toMillis()
                );
            }
        }
    }

    /**
     * Execute method with TypeReference and retry
     */
    private <T> T executeMethodWithRetry(String method, Map<String, Object> params,
                                         TypeReference<T> typeReference) throws IOException {
        return executeMethodWithRetry(method, params, response ->
                objectMapper.readValue(response, typeReference));
    }

    /**
     * Execute method without retry (for long polling), using a specific client instance
     */
    private <T> T executeMethodInternal(OkHttpClient client, String method, Map<String, Object> params,
                                        TypeReference<T> typeReference) throws IOException {
        String response = executeHttpRequest(client, method, params);
        return objectMapper.readValue(response, typeReference);
    }

    /**
     * Execute HTTP request using the provided client and return response body
     */
    private String executeHttpRequest(OkHttpClient client, String method, Map<String, Object> params) throws IOException {
        String url = API_URL + botToken + "/" + method;
        String jsonParams = objectMapper.writeValueAsString(params);

        RequestBody body = RequestBody.create(jsonParams, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            ResponseBody responseBody = response.body();
            String responseString = responseBody != null ? responseBody.string() : "";

            if (statusCode >= 500) {
                throw new IOException("Server error: " + statusCode + ", body: " + responseString);
            }

            return responseString;
        } catch (InterruptedIOException e) {
            // Preserve InterruptedIOException for getUpdates long polling interruption handling
            throw e;
        }
    }

    /**
     * Check if error is retryable
     */
    private boolean isRetryableError(IOException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Server error")
                || message.contains("Connection")
                || message.contains("timeout")
                || message.contains("Timeout");
    }

    /**
     * Functional interface for response parsing
     */
    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String response) throws IOException;
    }

    /**
     * Close OkHttpClient resources.
     * Only closes if this instance owns the HTTP client.
     */
    @Override
    public void close() {
        if (ownsHttpClient && httpClient != null) {
            log.debug("Shutting down OkHttpClient");
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}