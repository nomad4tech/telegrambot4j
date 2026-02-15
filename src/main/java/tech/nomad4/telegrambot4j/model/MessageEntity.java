package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram MessageEntity object
 * <a href="https://core.telegram.org/bots/api#messageentity">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageEntity {

    @JsonProperty("type")
    private String type; // mention, hashtag, bot_command, etc.

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("url")
    private String url;

    @JsonProperty("user")
    private User user;
}

