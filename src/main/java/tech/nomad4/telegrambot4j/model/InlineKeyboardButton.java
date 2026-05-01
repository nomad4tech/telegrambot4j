package tech.nomad4.telegrambot4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents one button of an inline keyboard.
 * <a href="https://core.telegram.org/bots/api#inlinekeyboardbutton">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineKeyboardButton {

    @JsonProperty("text")
    private String text;

    @JsonProperty("callback_data")
    private String callbackData;

    @JsonProperty("url")
    private String url;

    /**
     * Creates a button that sends callback data when pressed.
     */
    public static InlineKeyboardButton callback(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }

    /**
     * Creates a button that opens a URL when pressed.
     */
    public static InlineKeyboardButton url(String text, String url) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setUrl(url);
        return btn;
    }
}
