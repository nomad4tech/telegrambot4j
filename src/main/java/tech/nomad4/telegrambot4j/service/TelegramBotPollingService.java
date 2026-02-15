package tech.nomad4.telegrambot4j.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.nomad4.telegrambot4j.api.TelegramApiClient;
import tech.nomad4.telegrambot4j.model.Update;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for receiving updates via long polling
 * Universal POJO - works both standalone and in Spring
 */
@Slf4j
@Getter
@Setter
public class TelegramBotPollingService {

    private final TelegramApiClient apiClient;
    private final UpdateHandler updateHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong offset = new AtomicLong(0);

    private volatile Thread pollingThread;

    private String botName = "default";
    private int pollingTimeout = 30;
    private int errorDelay = 3000;

    /**
     * Create polling service with mandatory autoStart specification
     *
     * @param apiClient Telegram API client
     * @param updateHandler update handler (usually UpdateDispatcher)
     * @param autoStart start automatically upon creation
     */
    public TelegramBotPollingService(TelegramApiClient apiClient, UpdateHandler updateHandler, boolean autoStart) {
        this.apiClient = apiClient;
        this.updateHandler = updateHandler;

        if (autoStart) {
            log.info("Auto-starting bot polling...");
            startPollingAsync();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered, stopping bot: {}", botName);
                shutdown();
            }, "bot-shutdown-" + botName));
        }
    }

    /**
     * Start polling in current thread
     * Blocking method
     */
    public void startPolling() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Polling already running");
        }

        pollingThread = Thread.currentThread();
        log.info("Starting Telegram bot polling...");

        try {
            while (running.get()) {
                try {
                    List<Update> updates = apiClient.getUpdates(
                        offset.get() > 0 ? offset.get() : null,
                        null,
                        pollingTimeout
                    );

                    if (updates != null && !updates.isEmpty()) {
                        for (Update update : updates) {
                            try {
                                updateHandler.handle(update);
                            } catch (Exception e) {
                                log.error("Error handling update: {}", e.getMessage(), e);
                            }

                            offset.set(update.getUpdateId() + 1);
                        }
                    }

                } catch (InterruptedIOException e) {
                    log.debug("Polling interrupted (normal shutdown)");
                    break;
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted() || !running.get()) {
                        log.debug("Polling interrupted during IO error handling");
                        break;
                    }

                    log.error("Error getting updates: {}", e.getMessage());
                    try {
                        Thread.sleep(errorDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.debug("Sleep interrupted (normal shutdown)");
                        break;
                    }
                }
            }
        } finally {
            pollingThread = null;
            running.set(false);
            log.info("Telegram bot polling stopped");
        }
    }

    /**
     * Start polling in separate thread
     * @param daemon if true, thread will be daemon (terminates when main thread exits)
     */
    public Thread startPollingAsync(boolean daemon) {
        Thread thread = new Thread(this::startPolling, "telegram-bot-polling");
        thread.setDaemon(daemon);
        thread.start();
        return thread;
    }

    /**
     * Start polling in separate thread (non-daemon - keeps application running)
     */
    public Thread startPollingAsync() {
        return startPollingAsync(false);
    }

    /**
     * Stop polling immediately
     * Interrupts the polling thread if blocked on long polling request
     */
    public void stopPolling() {
        if (running.get()) {
            log.info("Stopping bot polling for: {}", botName);
        }
        running.set(false);

        Thread thread = pollingThread;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    /**
     * Stop on shutdown (for graceful shutdown)
     * Called automatically via shutdown hook or @PreDestroy in Spring
     */
    public void shutdown() {
        stopPolling();
    }

    /**
     * Check if polling is running
     */
    public boolean isRunning() {
        return running.get();
    }

}

