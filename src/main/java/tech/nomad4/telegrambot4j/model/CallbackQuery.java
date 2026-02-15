package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram CallbackQuery object
 * <a href="https://core.telegram.org/bots/api#callbackquery">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("from")
    private User from;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("inline_message_id")
    private String inlineMessageId;

    @JsonProperty("chat_instance")
    private String chatInstance;

    @JsonProperty("data")
    private String data;
}

