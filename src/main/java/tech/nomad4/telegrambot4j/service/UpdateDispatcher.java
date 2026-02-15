package tech.nomad4.telegrambot4j.service;

import lombok.extern.slf4j.Slf4j;
import tech.nomad4.telegrambot4j.model.Update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dispatcher for managing multiple update handlers
 * Allows adding multiple handlers and invoking them in a chain
 */
@Slf4j
public class UpdateDispatcher implements UpdateHandler {

    private final List<UpdateHandler> handlers;

    /**
     * Create dispatcher with mandatory list of handlers
     * This ensures the user explicitly provides at least one handler
     *
     * @param handlers list of handlers (cannot be null or empty)
     * @throws IllegalArgumentException if list is empty or null
     */
    public UpdateDispatcher(List<UpdateHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException(
                "No UpdateHandler provided. Implement UpdateHandler interface and register at least one handler."
            );
        }
        this.handlers = new ArrayList<>(handlers);
        log.debug("UpdateDispatcher created with {} handler(s)", handlers.size());
    }

    /**
     * Create dispatcher with array of handlers
     *
     * @param handlers array of handlers
     * @throws IllegalArgumentException if array is empty or null
     */
    public UpdateDispatcher(UpdateHandler... handlers) {
        this(handlers != null ? Arrays.asList(handlers) : null);
    }

    /**
     * Add handler
     */
    public UpdateDispatcher addHandler(UpdateHandler handler) {
        handlers.add(handler);
        return this;
    }

    /**
     * Remove handler
     */
    public UpdateDispatcher removeHandler(UpdateHandler handler) {
        handlers.remove(handler);
        return this;
    }

    /**
     * Handle update with all registered handlers
     * Stops on first handler that returns true
     * @return true if any handler processed the update, false otherwise
     */
    @Override
    public boolean handle(Update update) {
        boolean handled = false;
        for (UpdateHandler handler : handlers) {
            try {
                if (handler.handle(update)) {
                    handled = true;
                    break;
                }
            } catch (Exception e) {
                log.error("Error in handler {}: {}", handler.getClass().getName(), e.getMessage(), e);
            }
        }
        return handled;
    }

    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Get handlers registry for convenient registration
     *
     * @return HandlersRegistry for chain registration of handlers
     */
    public HandlersRegistry getRegistry() {
        return new HandlersRegistry(this);
    }
}

