package e1.engine;
import dev.langchain4j.model.chat.ChatModel;
import e1.model.Player;
import e1.model.StoryResponse;
import e1.prompt.StoryPrompt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummarisingStoryMemoryTest {

    @Mock
    private ChatModel mockModel;

    @Mock
    private StoryPrompt mockPrompt;

    @Test
    void shouldInitiallyHaveNoPreviousStoryEvents() {
        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        assertEquals("No previous story events.", memory.context());
    }

    @Test
    void shouldUpdateSummaryUsingModel() {
        when(mockModel.chat(anyString()))
                .thenReturn("The player found a silver key.");

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        memory.remember(
                mockPrompt,
                response("The player found a silver key.")
        );

        assertEquals("The player found a silver key.", memory.context());

        verify(mockModel, times(1)).chat(anyString());
    }

    @Test
    void shouldIncludeNewStoryBeatInSummarisationPrompt() {
        when(mockModel.chat(anyString()))
                .thenReturn("The player entered the forest and found a silver key.");

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        memory.remember(
                mockPrompt,
                response("The player entered the forest and found a silver key.")
        );

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockModel).chat(captor.capture());

        String summarisationPrompt = captor.getValue();

        assertTrue(summarisationPrompt.contains("The player entered the forest"));
        assertTrue(summarisationPrompt.contains("silver key"));
        assertTrue(summarisationPrompt.contains("P"));
        assertTrue(summarisationPrompt.contains("health"));
        assertTrue(summarisationPrompt.contains("attackPower"));
    }

    @Test
    void shouldUseExistingSummaryWhenCreatingNextSummary() {
        when(mockModel.chat(anyString()))
                .thenReturn("The player found a key.")
                .thenReturn("The player found a key and opened the castle gate.");

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        memory.remember(
                mockPrompt,
                response("The player found a key.")
        );

        memory.remember(
                mockPrompt,
                response("The player opened the castle gate.")
        );

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockModel, times(2)).chat(captor.capture());

        String secondSummarisationPrompt = captor.getAllValues().get(1);

        assertTrue(secondSummarisationPrompt.contains("The player found a key."));
        assertTrue(secondSummarisationPrompt.contains("The player opened the castle gate."));

        assertEquals(
                "The player found a key and opened the castle gate.",
                memory.context()
        );
    }

    @Test
    void shouldMeasureApproximateTokenCost() {
        when(mockModel.chat(anyString()))
                .thenReturn("The player found a key.");

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        memory.remember(
                mockPrompt,
                response("The player found a key.")
        );

        assertEquals(memory.context().length() / 4, memory.approximateTokenCost());
    }

    @Test
    void shouldHaveLowerTokenCostThanLongFixedWindowMemory() {
        StoryMemory fixedWindowMemory = new StoryMemory(10);

        when(mockModel.chat(anyString()))
                .thenReturn("Player has key. Player is in castle.");

        SummarisingStoryMemory summarisingMemory = new SummarisingStoryMemory(mockModel);

        StoryResponse longResponse = response("""
                The player walked through the ancient forest, discovered a silver key,
                met a wounded knight, learned about the locked castle gate,
                saw a dragon sleeping near the bridge, and decided to continue
                toward the mountain fortress.
                """);

        fixedWindowMemory.remember(longResponse);
        summarisingMemory.remember(mockPrompt, longResponse);
        System.out.println(summarisingMemory.context().length()); //36
        System.out.println(fixedWindowMemory.context().length()); //343
        System.out.println(summarisingMemory.approximateTokenCost()); // 9 
        System.out.println(fixedWindowMemory.approximateTokenCost()); // 85
        assertTrue(summarisingMemory.context().length() < fixedWindowMemory.context().length());
        assertTrue(summarisingMemory.approximateTokenCost() < fixedWindowMemory.approximateTokenCost());
    }

    @Test
    void shouldCallSummariserOncePerRememberedBeat() {
        when(mockModel.chat(anyString()))
                .thenReturn("Summary 1")
                .thenReturn("Summary 2")
                .thenReturn("Summary 3");

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        memory.remember(mockPrompt, response("First event."));
        memory.remember(mockPrompt, response("Second event."));
        memory.remember(mockPrompt, response("Third event."));

        verify(mockModel, times(3)).chat(anyString());

        assertEquals(3, memory.getCalls());
    }

    @Test
    void shouldPropagateExceptionIfSummariserFails() {
        when(mockModel.chat(anyString()))
                .thenThrow(new IllegalStateException("Summariser failed"));

        SummarisingStoryMemory memory = new SummarisingStoryMemory(mockModel);

        assertThrows(
                IllegalStateException.class,
                () -> memory.remember(mockPrompt, response("The player found a key."))
        );

        assertEquals("No previous story events.", memory.context());
    }

    private StoryResponse response(String narrative) {
        return new StoryResponse(
                narrative,
                "What do you do next?",
                List.of("Look around", "Move forward", "Call for help"),
                new Player("P", 100, 10),
                false
        );
    }
}
