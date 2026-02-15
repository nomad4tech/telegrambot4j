package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram Message object
 * <a href="https://core.telegram.org/bots/api#message">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    @JsonProperty("message_id")
    private Integer messageId;

    @JsonProperty("from")
    private User from;

    @JsonProperty("chat")
    private Chat chat;

    @JsonProperty("date")
    private Long date;

    @JsonProperty("text")
    private String text;

    @JsonProperty("reply_to_message")
    private Message replyToMessage;

    @JsonProperty("entities")
    private MessageEntity[] entities;
}

