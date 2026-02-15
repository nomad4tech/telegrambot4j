package tech.nomad4.telegrambot4j.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Handlers registry - simplifies registration of multiple handlers
 * Can be used in both Spring and standalone applications
 */
@Slf4j
@RequiredArgsConstructor
public class HandlersRegistry {

    private final UpdateDispatcher dispatcher;

    /**
     * Register single handler
     */
    public HandlersRegistry register(UpdateHandler handler) {
        // Skip dispatcher itself to avoid recursion
        if (handler instanceof UpdateDispatcher) {
            log.debug("Skipping UpdateDispatcher registration (avoided recursion)");
            return this;
        }

        dispatcher.addHandler(handler);
        log.debug("Registered handler: {}", handler.getClass().getSimpleName());
        return this;
    }

    /**
     * Register multiple handlers
     */
    public HandlersRegistry register(UpdateHandler... handlers) {
        for (UpdateHandler handler : handlers) {
            register(handler);
        }
        return this;
    }

    /**
     * Register list of handlers
     */
    public HandlersRegistry register(List<UpdateHandler> handlers) {
        handlers.forEach(this::register);
        return this;
    }

    /**
     * Automatically find and register handlers via reflection
     * Scans specified package and registers all classes implementing UpdateHandler
     *
     * @param packageName package name to scan (e.g. "com.mycompany.bot.handlers")
     */
    public HandlersRegistry scanAndRegister(String packageName) {
        List<UpdateHandler> handlers = HandlerScanner.scanPackage(packageName);

        if (handlers.isEmpty()) {
            log.warn("No UpdateHandler implementations found in package: {}", packageName);
        } else {
            register(handlers);
            log.info("Registered {} handler(s) from package: {}", handlers.size(), packageName);
        }

        return this;
    }

    /**
     * Get number of registered handlers
     */
    public int getHandlerCount() {
        return dispatcher.getHandlerCount();
    }
}

