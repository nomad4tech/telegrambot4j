package tech.nomad4.telegrambot4j.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Telegram ResponseParameters object - extra info Telegram attaches to some failed requests.
 * <a href="https://core.telegram.org/bots/api#responseparameters">...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseParameters {

    @JsonProperty("retry_after")
    private Integer retryAfter;
}
