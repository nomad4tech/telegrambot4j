package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram ChatMemberUpdated object - represents changes in the status of a chat member,
 * including the bot's own status in a private chat (sent as {@code my_chat_member}), e.g.
 * a user blocking or unblocking the bot ({@code new_chat_member.status} becomes "kicked"
 * or "member" respectively).
 * <a href="https://core.telegram.org/bots/api#chatmemberupdated">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMemberUpdated {

    @JsonProperty("chat")
    private Chat chat;

    @JsonProperty("from")
    private User from;

    @JsonProperty("date")
    private Long date;

    @JsonProperty("old_chat_member")
    private ChatMember oldChatMember;

    @JsonProperty("new_chat_member")
    private ChatMember newChatMember;
}
