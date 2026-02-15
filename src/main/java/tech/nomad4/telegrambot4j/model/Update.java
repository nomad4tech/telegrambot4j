package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram Update object
 * <a href="https://core.telegram.org/bots/api#update">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Update {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("edited_message")
    private Message editedMessage;

    @JsonProperty("channel_post")
    private Message channelPost;

    @JsonProperty("edited_channel_post")
    private Message editedChannelPost;

    @JsonProperty("callback_query")
    private CallbackQuery callbackQuery;
}

