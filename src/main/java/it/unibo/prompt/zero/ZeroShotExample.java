package it.unibo.prompt.zero;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import it.unibo.utils.LlmConstants;

public class ZeroShotExample {
    public static void main(String[] args) {
        final ChatModel model = OllamaChatModel.builder()
            .baseUrl(LlmConstants.OLLAMA_BASE_URL)
            .logRequests(true)
            .logResponses(true)
            .think(false)     
            .modelName(LlmConstants.CHAT_MODEL_QWEN)
            .numPredict(256)
            .temperature(0.0)
            .build();
        final var zeroShot = new ZeroShotAgent(model, "Just reply with the RIGHT number.");
        final var query = """
            Q: Today I have 6 apples. Tomorrow I buy 3 more. Yesterday I ate 6
            apples, How many apples do I have (today)?
        """;
        for (int i = 0; i < 20; i++) {
            var response = zeroShot.ask(query);
            System.out.println("Response: " + response);
        }
    }
}
