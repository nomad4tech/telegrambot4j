package tech.nomad4.telegrambot4j.service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Scanner for automatic discovery of UpdateHandler implementations
 */
@Slf4j
class HandlerScanner {

    private HandlerScanner() {
        /* This utility class should not be instantiated */
    }

    /**
     * Scans specified package and returns instances of all classes implementing UpdateHandler
     *
     * @param packageName package name to scan
     * @return list of UpdateHandler instances
     */
    static List<UpdateHandler> scanPackage(String packageName) {
        List<UpdateHandler> handlers = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                log.warn("Package not found: {}", packageName);
                return handlers;
            }

            File directory = new File(resource.getFile());

            if (directory.exists()) {
                scanDirectory(directory, packageName, handlers);
            }

        } catch (Exception e) {
            log.error("Error scanning package {}: {}", packageName, e.getMessage(), e);
        }

        return handlers;
    }

    private static void scanDirectory(File directory, String packageName, List<UpdateHandler> handlers) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), handlers);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                tryCreateHandler(className, handlers);
            }
        }
    }

    private static void tryCreateHandler(String className, List<UpdateHandler> handlers) {
        try {
            Class<?> clazz = Class.forName(className);

            if (!clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())
                && UpdateHandler.class.isAssignableFrom(clazz)) {

                try {
                    UpdateHandler handler = (UpdateHandler) clazz.getDeclaredConstructor().newInstance();
                    handlers.add(handler);
                    log.debug("Found and instantiated handler: {}", clazz.getSimpleName());
                } catch (NoSuchMethodException e) {
                    log.debug("Handler {} has no default constructor, skipping", clazz.getSimpleName());
                }
            }

        } catch (Exception e) {
            log.debug("Could not load class {}: {}", className, e.getMessage());
        }
    }
}

