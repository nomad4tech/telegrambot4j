# telegrambot4j

> Built for my own project and use, polished and open sourced. Use at your own risk - but it works fine for me.

Lightweight wrapper over Telegram Bot API for Java. Pure POJO - works standalone or in Spring Boot. Provides production-ready HTTP client with connection pooling, retry logic, and handler chain pattern for processing updates. No framework dependencies - only OkHttp, Jackson, and Lombok.

## Features

- **Handler chain pattern** - `UpdateHandler` returns `boolean`, dispatcher stops on first `true`
- **Production-ready HTTP client** - OkHttp with connection pooling, configurable timeouts, keep-alive, exponential backoff retry
- **Auto-adjusted polling timeout** - OkHttp readTimeout automatically adjusted per-request to prevent connection drops during long polling
- **Inline keyboard support** - `InlineKeyboardMarkup` with callback buttons and URL buttons
- **Callback query handling** - `CallbackQuery` model + `answerCallbackQuery` method
- **Pure POJO** - no Spring dependencies in core, but Spring-friendly
- **Long polling service** - automatic offset management, immediate shutdown via thread interruption
- **Fluent registration API** - `HandlersRegistry` for convenient handler registration
- **Auto-discovery** - `scanAndRegister(packageName)` finds handlers via reflection

## Maven Dependency

```xml
<dependency>
    <groupId>tech.nomad4</groupId>
    <artifactId>telegrambot4j</artifactId>
    <version>1.1.0</version>
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

See [nomad4tech/telegrambot4j-demo](https://github.com/nomad4tech/telegrambot4jdemo) for working Spring Boot bot built with this library.

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

## Inline Keyboards

Send messages with inline buttons:

```java
// Single callback button
InlineKeyboardButton button = InlineKeyboardButton.callback("Click me", "my_callback_data");

// URL button
InlineKeyboardButton urlButton = InlineKeyboardButton.url("Open link", "https://example.com");

// Single column keyboard (each button on its own row)
InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.singleColumn(List.of(
    InlineKeyboardButton.callback("Option 1", "option_1"),
    InlineKeyboardButton.callback("Option 2", "option_2"),
    InlineKeyboardButton.callback("❌ Delete", "delete_item")
));

// Custom layout (rows defined manually)
InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.of(List.of(
    List.of(
        InlineKeyboardButton.callback("Yes", "confirm_yes"),
        InlineKeyboardButton.callback("No", "confirm_no")
    ),
    List.of(
        InlineKeyboardButton.url("Learn more", "https://example.com")
    )
));

// Send message with keyboard
apiClient.sendMessage(chatId, "Choose an option:", keyboard);
```

## Handling Callback Queries

When a user presses an inline button, a `CallbackQuery` arrives in the update:

```java
public class MyCallbackHandler implements UpdateHandler {

    private final TelegramApiClient apiClient;

    public MyCallbackHandler(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public boolean handle(Update update) {
        if (update.getCallbackQuery() == null) {
            return false;
        }

        CallbackQuery callback = update.getCallbackQuery();
        String data = callback.getData();
        Long chatId = callback.getMessage().getChat().getId();
        Long userId = callback.getFrom().getId();

        try {
            if ("delete_item".equals(data)) {
                // handle deletion
                apiClient.answerCallbackQuery(callback.getId(), "Deleted!", false);
            }
        } catch (IOException e) {
            // log error
        }

        return true;
    }
}
```

Always call `answerCallbackQuery` after handling — otherwise the loading spinner stays on the button.

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

**application.properties:**

```properties
telegram.bot.token=${BOT_TOKEN}
spring.main.keep-alive=true
```

> **Important:** `spring.main.keep-alive=true` prevents Spring Boot from shutting down after startup. Without it, the application will exit immediately even though the polling thread is running.

## Configuration

```java
TelegramApiConfig config = TelegramApiConfig.builder()
    .maxIdleConnections(10)
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(60))
    .writeTimeout(Duration.ofSeconds(60))
    .keepAlive(Duration.ofSeconds(60))
    .maxRetryAttempts(5)
    .build();

TelegramApiClient apiClient = new TelegramApiClient(botToken, config);
```

Defaults: 5 idle connections in OkHttp ConnectionPool, 30s keep-alive, 10s connect timeout, 30s read/write timeout, 3 retry attempts with exponential backoff starting at 500ms.

> **Note on readTimeout:** For long polling (`getUpdates`), the library automatically adjusts OkHttp's readTimeout per-request to ensure it's always greater than the polling timeout. This prevents premature connection drops.

Or provide fully custom OkHttpClient:

```java
OkHttpClient customClient = new OkHttpClient.Builder()
    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080)))
    .connectTimeout(15, TimeUnit.SECONDS)
    .build();

TelegramApiClient apiClient = new TelegramApiClient(botToken, customClient);
```

## API Reference

### TelegramApiClient methods

| Method | Description |
|--------|-------------|
| `sendMessage(chatId, text)` | Send plain text message |
| `sendMessage(chatId, text, keyboard)` | Send message with inline keyboard |
| `sendMessage(chatId, text, parseMode, replyToMessageId, replyMarkup)` | Send message with all parameters |
| `answerCallbackQuery(callbackQueryId, text, showAlert)` | Answer inline button press |
| `getUpdates(offset, limit, timeout)` | Get updates via long polling |
| `getMe()` | Get bot info |
| `executeMethod(method, params, responseType)` | Execute any Telegram API method |

### Models

| Class | Description |
|-------|-------------|
| `Update` | Incoming update (message, callback_query, etc.) |
| `Message` | Text message with sender, chat, date |
| `CallbackQuery` | Inline button press event |
| `InlineKeyboardMarkup` | Inline keyboard layout |
| `InlineKeyboardButton` | Single inline button (callback or URL) |
| `Chat` | Chat info (id, type, title) |
| `User` | User info (id, username, name) |

## Changelog

### 1.1.0
- Added `InlineKeyboardButton` with `callback()` and `url()` factory methods
- Added `InlineKeyboardMarkup` with `of()` and `singleColumn()` factory methods
- Added `sendMessage(chatId, text, InlineKeyboardMarkup)` convenience method
- `answerCallbackQuery` was already supported in 1.0.0

### 1.0.0
- Initial release

## License

MIT - see [LICENSE](LICENSE) in repository root.