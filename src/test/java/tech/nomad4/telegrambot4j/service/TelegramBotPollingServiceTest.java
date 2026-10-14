package tech.nomad4.telegrambot4j.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.nomad4.telegrambot4j.api.TelegramApiClient;
import tech.nomad4.telegrambot4j.model.Update;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The polling loop must survive transient errors on {@code getUpdates} and stop only when asked to.
 *
 * <p>The regression these tests pin down: OkHttp reports a read/connect timeout as
 * {@link SocketTimeoutException}, which extends {@link InterruptedIOException}; the loop used to
 * treat any {@code InterruptedIOException} as a normal shutdown, so a single timeout ended polling
 * for good while the application kept running and the bot silently stopped receiving updates.
 */
class TelegramBotPollingServiceTest {

    private TelegramBotPollingService service;
    private ScriptedClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (service != null) {
            service.shutdown();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void readTimeout_isTransient_pollingKeepsRunningAndDeliversNextUpdate() throws Exception {
        CountDownLatch delivered = new CountDownLatch(1);
        List<Long> handled = new CopyOnWriteArrayList<>();
        client = new ScriptedClient(
                () -> { throw new SocketTimeoutException("timeout"); },
                () -> List.of(update(7L)));
        service = new TelegramBotPollingService(client, u -> {
            handled.add(u.getUpdateId());
            delivered.countDown();
            return true;
        }, false);
        service.setErrorDelay(10);

        service.startPollingAsync(true);

        assertThat(delivered.await(5, TimeUnit.SECONDS)).as("update after a timeout is delivered").isTrue();
        assertThat(service.isRunning()).isTrue();
        assertThat(handled).containsExactly(7L);
    }

    @Test
    void serverError_isTransient_pollingKeepsRunning() throws Exception {
        CountDownLatch delivered = new CountDownLatch(1);
        client = new ScriptedClient(
                () -> { throw new IOException("Server error: 502, body: Bad Gateway"); },
                () -> List.of(update(1L)));
        service = new TelegramBotPollingService(client, u -> {
            delivered.countDown();
            return true;
        }, false);
        service.setErrorDelay(10);

        service.startPollingAsync(true);

        assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(service.isRunning()).isTrue();
    }

    @Test
    void unexpectedRuntimeException_doesNotEndPolling() throws Exception {
        CountDownLatch delivered = new CountDownLatch(1);
        client = new ScriptedClient(
                () -> { throw new IllegalStateException("boom"); },
                () -> List.of(update(3L)));
        service = new TelegramBotPollingService(client, u -> {
            delivered.countDown();
            return true;
        }, false);
        service.setErrorDelay(10);

        service.startPollingAsync(true);

        assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(service.isRunning()).isTrue();
    }

    @Test
    void stopPolling_whileBlockedInRequest_endsPollingForReal() throws Exception {
        CountDownLatch inRequest = new CountDownLatch(1);
        client = new ScriptedClient(() -> {
            inRequest.countDown();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                // what OkHttp does when the thread is interrupted during blocking IO
                throw new InterruptedIOException("interrupted");
            }
            return List.of();
        });
        service = new TelegramBotPollingService(client, u -> true, false);
        service.setErrorDelay(10);
        Thread thread = service.startPollingAsync(true);
        assertThat(inRequest.await(5, TimeUnit.SECONDS)).isTrue();

        service.stopPolling();

        thread.join(5_000);
        assertThat(thread.isAlive()).as("polling thread ended").isFalse();
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void stopPolling_duringErrorDelay_endsPolling() throws Exception {
        CountDownLatch failed = new CountDownLatch(1);
        client = new ScriptedClient(() -> {
            failed.countDown();
            throw new SocketTimeoutException("timeout");
        });
        service = new TelegramBotPollingService(client, u -> true, false);
        service.setErrorDelay(60_000);
        Thread thread = service.startPollingAsync(true);
        assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue();

        service.stopPolling();

        thread.join(5_000);
        assertThat(thread.isAlive()).isFalse();
        assertThat(service.isRunning()).isFalse();
    }

    private static Update update(long id) {
        Update u = new Update();
        u.setUpdateId(id);
        return u;
    }

    @FunctionalInterface
    private interface Step {
        List<Update> run() throws IOException;
    }

    /** Plays the scripted steps in order, then keeps returning empty batches after a short pause. */
    private static final class ScriptedClient extends TelegramApiClient {
        private final Step[] steps;
        private final AtomicInteger calls = new AtomicInteger();

        ScriptedClient(Step... steps) {
            super("test-token");
            this.steps = steps;
        }

        @Override
        public List<Update> getUpdates(Long offset, Integer limit, Integer timeout) throws IOException {
            int i = calls.getAndIncrement();
            if (i < steps.length) {
                return steps[i].run();
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new InterruptedIOException("interrupted");
            }
            return List.of();
        }
    }
}
