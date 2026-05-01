# Changelog

All notable changes to telegrambot4j will be documented in this file.


## [1.1.1] - 2026-05-01

### Fixed
- InlineKeyboardButton.java - Include non-null properties in JSON serialization

---

## [1.1.0] - 2026-05-01

### Added
- `InlineKeyboardButton` model with factory methods:
  - `callback(text, callbackData)` - button that sends callback data
  - `url(text, url)` - button that opens a URL
- `InlineKeyboardMarkup` model with factory methods:
  - `of(rows)` - custom keyboard layout
  - `singleColumn(buttons)` - each button in its own row
- `sendMessage(chatId, text, InlineKeyboardMarkup)` - convenience method for sending messages with inline keyboards
- GitHub Actions workflow for automatic publishing to GitHub Packages on tag push

### Notes
- `answerCallbackQuery` and `CallbackQuery` model were already present in 1.0.0
- `Update` already included `callbackQuery` field in 1.0.0

---

## [1.0.0] - 2026-02-15

### Added
- `UpdateHandler` chain - `boolean handle()`, dispatcher stops on first `true`
- `UpdateDispatcher` with fluent registration API via `HandlersRegistry`
- `TelegramApiClient` - OkHttp with connection pooling, retry logic, exponential backoff
- `TelegramBotPollingService` - long polling, automatic offset management, graceful shutdown
- `CallbackQuery`, `Message`, `Update`, `Chat`, `User`, `MessageEntity` models
- `answerCallbackQuery` method
- Spring Boot friendly - pure POJO, no framework dependencies
