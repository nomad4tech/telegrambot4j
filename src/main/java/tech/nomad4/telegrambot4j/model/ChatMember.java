package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram ChatMember object
 * <a href="https://core.telegram.org/bots/api#chatmember">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMember {

    @JsonProperty("status")
    private String status; // creator, administrator, member, restricted, left, kicked

    @JsonProperty("user")
    private User user;
}
