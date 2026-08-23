package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the JSON shapes Telegram actually sends map onto the model classes correctly -
 * particularly {@code my_chat_member}, since a silent mapping gap here (e.g. a typo in a
 * {@code @JsonProperty} name) would fail closed: {@code ignoreUnknown = true} means Jackson
 * just leaves the field {@code null} instead of throwing, so a broken mapping would go
 * unnoticed without a test asserting the field round-trips as an actual object.
 */
class UpdateDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void myChatMember_userBlockedBot_mapsStatusAndUser() throws Exception {
        String json = """
                {
                  "update_id": 123456,
                  "my_chat_member": {
                    "chat": {"id": 987654321, "type": "private", "first_name": "Alex"},
                    "from": {"id": 987654321, "is_bot": false, "first_name": "Alex"},
                    "date": 1735689600,
                    "old_chat_member": {
                      "status": "member",
                      "user": {"id": 111222333, "is_bot": true, "first_name": "MyBot"}
                    },
                    "new_chat_member": {
                      "status": "kicked",
                      "user": {"id": 111222333, "is_bot": true, "first_name": "MyBot"}
                    }
                  }
                }
                """;

        Update update = objectMapper.readValue(json, Update.class);

        assertThat(update.getMyChatMember()).isNotNull();
        assertThat(update.getMyChatMember().getChat().getId()).isEqualTo(987654321L);
        assertThat(update.getMyChatMember().getOldChatMember().getStatus()).isEqualTo("member");
        assertThat(update.getMyChatMember().getNewChatMember().getStatus()).isEqualTo("kicked");
        assertThat(update.getMyChatMember().getNewChatMember().getUser().getId()).isEqualTo(111222333L);
    }

    @Test
    void myChatMember_userUnblockedBot_statusBecomesMember() throws Exception {
        String json = """
                {
                  "update_id": 123457,
                  "my_chat_member": {
                    "chat": {"id": 987654321, "type": "private", "first_name": "Alex"},
                    "from": {"id": 987654321, "is_bot": false, "first_name": "Alex"},
                    "date": 1735689700,
                    "old_chat_member": {
                      "status": "kicked",
                      "user": {"id": 111222333, "is_bot": true, "first_name": "MyBot"}
                    },
                    "new_chat_member": {
                      "status": "member",
                      "user": {"id": 111222333, "is_bot": true, "first_name": "MyBot"}
                    }
                  }
                }
                """;

        Update update = objectMapper.readValue(json, Update.class);

        assertThat(update.getMyChatMember().getOldChatMember().getStatus()).isEqualTo("kicked");
        assertThat(update.getMyChatMember().getNewChatMember().getStatus()).isEqualTo("member");
    }

    @Test
    void regularMessageUpdate_myChatMemberStaysNull() throws Exception {
        String json = """
                {
                  "update_id": 123458,
                  "message": {
                    "message_id": 1,
                    "from": {"id": 1, "is_bot": false, "first_name": "Alex"},
                    "chat": {"id": 1, "type": "private"},
                    "date": 1735689800,
                    "text": "hi"
                  }
                }
                """;

        Update update = objectMapper.readValue(json, Update.class);

        assertThat(update.getMyChatMember()).isNull();
        assertThat(update.getMessage()).isNotNull();
    }
}
