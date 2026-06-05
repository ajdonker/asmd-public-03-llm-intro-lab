package e1.engine;
import dev.langchain4j.model.chat.ChatModel;
import e1.model.Player;
import e1.model.StoryResponse;
import e1.prompt.BeginPrompt;
import e1.prompt.StoryPrompt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LLMStoryEngineTest {
    @Mock private ChatModel mockModel;
    private static final String VALID_JSON = """
        {
          "narrative": "You wake up in a dark forest.",
          "question": "What do you do next?",
          "choices": ["Look around", "Walk forward", "Call for help"],
          "updatedPlayer": {
            "name": "P",
            "health": 100,
            "attackPower": 10
          },
          "gameOver": false
        }
        """; 
    private static final JsonCodec jsonCodec = new JsonCodec();
    @Test
    void shouldParseValidJsonOnFirstTry() {
        when(mockModel.chat(anyString())).thenReturn(VALID_JSON);

        LLMStoryEngine engine = new LLMStoryEngine(mockModel, jsonCodec, 3);
        StoryPrompt prompt = new BeginPrompt(
                new Player("P",100,10),
                "A dark forest"
        );
        StoryResponse response = engine.request(prompt);
        assertEquals("You wake up in a dark forest.", response.narrative());
        assertEquals("What do you do next?", response.question());
        assertEquals(3, response.choices().size());
        assertEquals("P", response.updatedPlayer().name());
        assertEquals(100, response.updatedPlayer().health());
        assertEquals(10, response.updatedPlayer().attackPower());
        assertFalse(response.gameOver());

        verify(mockModel, times(1)).chat(anyString());
    }
    @Test
    void shouldRetryAndRecoverWhenLaterResponseIsValid() {
        when(mockModel.chat(anyString()))
                .thenReturn("garbage")
                .thenReturn(VALID_JSON);

        LLMStoryEngine engine = new LLMStoryEngine(mockModel,jsonCodec, 3);

        StoryPrompt prompt = new BeginPrompt(
                new Player("P", 100, 10),
                "A dark forest"
        );

        StoryResponse response = engine.request(prompt);

        assertEquals("You wake up in a dark forest.", response.narrative());
        assertFalse(response.gameOver());

        verify(mockModel, times(2)).chat(anyString());
    }
    @Test
    void shouldThrowWhenAllRetriesAreExhausted() {
        when(mockModel.chat(anyString()))
                .thenReturn("garbage")
                .thenReturn("still garbage")
                .thenReturn("not json");

        LLMStoryEngine engine = new LLMStoryEngine(mockModel,jsonCodec, 3);

        StoryPrompt prompt = new BeginPrompt(
                new Player("P", 100, 10),
                "A dark forest"
        );

        assertThrows(RuntimeException.class, () -> engine.request(prompt));

        verify(mockModel, times(3)).chat(anyString());
    }
    @Test
    void shouldRetryAfterMalformedJson() {
        String malformedJson = """
                {
                  "narrative": "Broken JSON",
                  "question": "Missing end"
                """;

        when(mockModel.chat(anyString()))
                .thenReturn(malformedJson)
                .thenReturn(VALID_JSON);

        LLMStoryEngine engine = new LLMStoryEngine(mockModel, jsonCodec, 3);

        StoryPrompt prompt = new BeginPrompt(
                new Player("P", 100, 10),
                "A dark forest"
        );
        StoryResponse response = engine.request(prompt);

        assertEquals("You wake up in a dark forest.", response.narrative());

        verify(mockModel, times(2)).chat(anyString());
    }

}
