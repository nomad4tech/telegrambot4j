package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram Chat object
 * <a href="https://core.telegram.org/bots/api#chat">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chat {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type; // private, group, supergroup, channel

    @JsonProperty("title")
    private String title;

    @JsonProperty("username")
    private String username;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;
}

