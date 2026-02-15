# telegrambot4j

> Built for my own project and use, polished and open sourced. Use at your own risk - but it works fine for me

Lightweight wrapper over Telegram Bot API for Java. Pure POJO - works standalone or in Spring Boot. Provides production-ready HTTP client with connection pooling, retry logic, and a handler chain pattern for processing updates. No framework dependencies - only OkHttp, Jackson, and Lombok.

## Features

- **Handler chain pattern** - `UpdateHandler` returns `boolean`, dispatcher stops on first `true`
- **Production-ready HTTP client** - OkHttp with connection pooling, configurable timeouts, keep-alive, exponential backoff retry
- **Pure POJO** - no Spring dependencies in core, but Spring-friendly
- **Long polling service** - automatic offset management, immediate shutdown via thread interruption
- **Fluent registration API** - `HandlersRegistry` for convenient handler registration
- **Auto-discovery** - `scanAndRegister(packageName)` finds handlers via reflection

## Maven Dependency

```xml
<dependency>
    <groupId>tech.nomad4</groupId>
    <artifactId>telegrambot4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

Add GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/nomad4tech/telegrambot4j</url>
    </repository>
</repositories>
```

## Quick Start

```java
public class MyBot {
    public static void main(String[] args) {
        String botToken = System.getenv("BOT_TOKEN");

        TelegramApiClient apiClient = new TelegramApiClient(botToken);

        UpdateHandler startHandler = update -> {
            if (update.getMessage() != null && "/start".equals(update.getMessage().getText())) {
                try {
                    apiClient.sendMessage(update.getMessage().getChat().getId(), "Hello!");
                    return true; // handled - stop the chain
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false; // not my update - pass to next handler
        };

        UpdateDispatcher dispatcher = new UpdateDispatcher(startHandler);

        new TelegramBotPollingService(apiClient, dispatcher, true); // autoStart = true
    }
}
```

## Handler Example

```java
public class StartCommandHandler implements UpdateHandler {

    private final TelegramApiClient apiClient;

    public StartCommandHandler(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public boolean handle(Update update) {
        if (update.getMessage() == null || update.getMessage().getText() == null) {
            return false;
        }

        if ("/start".equals(update.getMessage().getText())) {
            try {
                apiClient.sendMessage(
                        update.getMessage().getChat().getId(),
                        "Welcome! Use /help for available commands."
                );
                return true; // handled
            } catch (IOException e) {
                // log error
            }
        }

        return false; // not my command
    }
}
```

## How Dispatching Works

`UpdateDispatcher` maintains a list of `UpdateHandler` instances and invokes them sequentially. Each handler returns `boolean`: `true` means "I processed this update, stop the chain", `false` means "not mine, try next". This implements the Chain of Responsibility pattern - each update is processed by exactly one handler. The dispatcher itself implements `UpdateHandler`, so dispatchers can be nested.

## Registering Handlers

```java
// Via constructor
UpdateDispatcher dispatcher = new UpdateDispatcher(handler1, handler2, handler3);

// Via registry (fluent API)
dispatcher.getRegistry()
    .register(handler1)
    .register(handler2, handler3)
    .scanAndRegister("com.mybot.handlers"); // auto-discovery via reflection
```

> **Note:** `scanAndRegister` works with filesystem classpath only. It will not work inside a fat JAR (e.g. Spring Boot uber-jar). Use explicit registration in that case.

## Spring Boot Integration

```java
@Component
public class StartCommandHandler implements UpdateHandler {

    private final TelegramApiClient apiClient;

    public StartCommandHandler(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public boolean handle(Update update) {
        // ...
        return false;
    }
}

@Configuration
public class BotConfig {

    @Bean
    public TelegramApiClient apiClient(@Value("${bot.token}") String token) {
        return new TelegramApiClient(token);
    }

    @Bean
    public UpdateDispatcher dispatcher(List<UpdateHandler> handlers) {
        return new UpdateDispatcher(handlers); // Spring injects all UpdateHandler beans
    }

    @Bean
    public TelegramBotPollingService pollingService(TelegramApiClient client, UpdateDispatcher dispatcher) {
        return new TelegramBotPollingService(client, dispatcher, true);
    }
}
```

## Configuration

```java
TelegramApiConfig config = TelegramApiConfig.builder()
        .maxIdleConnections(10)    // OkHttp ConnectionPool max idle connections
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(60))
        .writeTimeout(Duration.ofSeconds(60))
        .keepAlive(Duration.ofSeconds(60))
        .maxRetryAttempts(5)
        .build();

TelegramApiClient apiClient = new TelegramApiClient(botToken, config);
```
Defaults: 5 idle connections in OkHttp ConnectionPool, 30s keep-alive, 10s connect timeout, 30s read/write timeout, 3 retry attempts with exponential backoff starting at 500ms.



Or provide fully custom OkHttpClient - useful for proxies, interceptors, custom SSL, etc.:
```java
OkHttpClient customClient = new OkHttpClient.Builder()
    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080)))
    .addInterceptor(chain -> {
        // custom interceptor logic
        return chain.proceed(chain.request());
    })
    .connectTimeout(15, TimeUnit.SECONDS)
    .build();

TelegramApiClient apiClient = new TelegramApiClient(botToken, customClient);
```


## Shutdown

```java
// Immediate stop - interrupts long polling HTTP request, does not wait up to 30s
pollingService.stopPolling();

// Release HTTP client resources
apiClient.close();
```

`TelegramApiClient` implements `Closeable`, so try-with-resources works too:

```java
try (TelegramApiClient apiClient = new TelegramApiClient(token)) {
UpdateDispatcher dispatcher = new UpdateDispatcher(myHandler);
    new TelegramBotPollingService(apiClient, dispatcher, true);
} // apiClient.close() called automatically
```

## License

MIT - see [LICENSE](LICENSE) in repository root.