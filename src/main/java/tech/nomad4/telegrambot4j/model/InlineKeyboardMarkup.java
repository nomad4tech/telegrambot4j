package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an inline keyboard that appears right next to the message it belongs to.
 * <a href="https://core.telegram.org/bots/api#inlinekeyboardmarkup">...</a>
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InlineKeyboardMarkup {

    @JsonProperty("inline_keyboard")
    private List<List<InlineKeyboardButton>> inlineKeyboard;

    /**
     * Creates an {@link InlineKeyboardMarkup} from a list of rows.
     */
    public static InlineKeyboardMarkup of(List<List<InlineKeyboardButton>> rows) {
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Creates an {@link InlineKeyboardMarkup} with each button placed in its own row:
     * {@code [[btn1], [btn2], [btn3]]}.
     */
    public static InlineKeyboardMarkup singleColumn(List<InlineKeyboardButton> buttons) {
        List<List<InlineKeyboardButton>> rows = buttons.stream()
                .map(btn -> List.of(btn))
                .collect(Collectors.toList());
        return new InlineKeyboardMarkup(rows);
    }
}
