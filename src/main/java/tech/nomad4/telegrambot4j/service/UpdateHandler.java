package tech.nomad4.telegrambot4j.service;


import tech.nomad4.telegrambot4j.model.Update;

/**
 * Interface for handling Telegram updates
 * Implementations of this interface will process incoming updates and determine if they should be handled or passed to other handlers
 * This allows for a flexible and modular approach to processing updates, where multiple handlers can be registered and invoked in a chain until one of them handles the update
 * The handle method returns a boolean indicating whether the update was handled (true) or should be passed to the next handler (false)
 */
@FunctionalInterface
public interface UpdateHandler {

    /**
     * Handles an incoming Telegram update
     * @param update the update to be processed
     * @return true if the update was handled and should not be processed by other handlers, false otherwise
     */
    boolean handle(Update update);
}

