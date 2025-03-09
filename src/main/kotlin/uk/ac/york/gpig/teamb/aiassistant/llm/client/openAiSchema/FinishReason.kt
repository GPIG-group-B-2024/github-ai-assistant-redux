package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The reason the model stopped generating a [Choice].
 *
 * Since we are not using tool calls or function calls, we will omit the `tool_calls` and `function_call` options for now.
 * */
enum class FinishReason {
    /**
     * This is the "good outcome" - the model has produced everything it wanted to produce
     * */
    @JsonProperty("stop")
    STOP,

    /**
     * Response was longer than allowed (will happen if we set a max response size in tokens and the model exceeds it)
     * */
    @JsonProperty(
        "length",
    )
    LENGTH,

    /**
     * The request contained something NSFW, unethical, etc.
     * */
    @JsonProperty(
        "content_filter",
    )
    CONTENT_FILTER,
}
