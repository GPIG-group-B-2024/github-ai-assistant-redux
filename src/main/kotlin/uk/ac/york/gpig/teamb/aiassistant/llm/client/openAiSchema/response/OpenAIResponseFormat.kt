package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.response

/**
 * This data class mirrors the "chat completion object" as outlined in the OpenAI [docs](https://platform.openai.com/docs/api-reference/chat/object).
 *
 * While there are other ways of obtaining responses (e.g. streaming them in [chunks](https://platform.openai.com/docs/api-reference/chat/streaming),
 * we will only utilise the most straightforward way for our MVP.
 *
 * Note: this class currently ignores some properties that may be present in the response.
 * In particular, those are:
 *  - `model` (we specify the model ourselves)
 *  - `object` (guaranteed to be `chat.completion` as we are only making requests to that endpoint)
 *  - `choices.logprobs` (we don't have a use case for this)
 *  - `usage.prompt_tokens_details` (as above)
 *  - `usage.completion_tokens_details` (as above + we are not planning to use reasoning/prediction features)
 *  - `system_fingerprint` (no use case)
 * */
data class OpenAIResponseFormat(
    val id: String,
    val created: Int,
    val choices: List<Choice>,
    val usage: Usage,
)
