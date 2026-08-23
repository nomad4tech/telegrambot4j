# Changelog

All notable changes to telegrambot4j will be documented in this file.


## [1.2.0] - 2026-08-24

### Added
- `ChatMember` and `ChatMemberUpdated` models
- `Update.myChatMember` field - delivered by Telegram whenever the bot's own status
  in a chat changes, e.g. a user blocking (`new_chat_member.status` becomes `"kicked"`)
  or unblocking (`"member"`) the bot in a private chat. No opt-in needed - `my_chat_member`
  is part of the default update set `getUpdates` already receives.
- Honor Telegram's `retry_after` on `429 Too Many Requests` (flood control) in
  `sendMessage`/`answerCallbackQuery`/`getMe` - previously a 429 wasn't even recognized
  as retryable (didn't match `isRetryableError`'s substring checks) and propagated
  straight to the caller instead of retrying after the server-specified wait.
  `TelegramRateLimitException` carries the `retry_after` value.
- First test suite for the project (JUnit 5 + AssertJ + MockWebServer)

### Fixed
- `ok=false` API responses (any error, not just 429) were only checked by the public
  convenience methods *after* `executeMethodWithRetry` already returned - meaning the
  retry loop itself never saw them, not just 429. Moved the check inside the loop via
  the new `executeTelegramMethodWithRetry`.

---

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
