package tech.nomad4.telegrambot4j.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Generic Telegram API Response wrapper
 * @param <T> Result type
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramResponse<T> {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("result")
    private T result;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("description")
    private String description;
}

