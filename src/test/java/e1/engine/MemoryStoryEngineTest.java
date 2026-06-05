package e1.engine;

import e1.model.Player;
import e1.model.StoryResponse;
import e1.prompt.StoryPrompt;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemoryStoryEngineTest {

    @Test
    void shouldInjectMemoryIntoPrompt() {
        StoryPrompt originalPrompt = mock(StoryPrompt.class);
        when(originalPrompt.toPromptString()).thenReturn("Original prompt");

        var decorated = new e1.prompt.MemoryPromptDecorator(
                originalPrompt,
                "The player found a silver key."
        );

        String result = decorated.toPromptString();

        assertTrue(result.contains("The player found a silver key."));
        assertTrue(result.contains("Original prompt"));
    }

    @Test
    void shouldCallDelegateWithDecoratedPrompt() {
        StoryEngine delegate = mock(StoryEngine.class);
        StoryMemory memory = new StoryMemory(5);

        StoryResponse response = new StoryResponse(
                "You wake up in a forest.",
                "What do you do?",
                List.of("Look around", "Walk forward", "Call for help"),
                new Player("P", 100, 10),
                false
        );

        when(delegate.request(any())).thenReturn(response);

        MemoryStoryEngineDec engine = new MemoryStoryEngineDec(delegate, memory);

        StoryPrompt prompt = mock(StoryPrompt.class);
        when(prompt.toPromptString()).thenReturn("Start story");

        StoryResponse result = engine.request(prompt);

        assertEquals(response, result);

        ArgumentCaptor<StoryPrompt> captor = ArgumentCaptor.forClass(StoryPrompt.class);
        verify(delegate).request(captor.capture());

        String sentPrompt = captor.getValue().toPromptString();

        assertTrue(sentPrompt.contains("No previous story events"));
        assertTrue(sentPrompt.contains("Start story"));
    }

    @Test
    void shouldRememberPreviousResponseForNextRequest() {
        StoryEngine delegate = mock(StoryEngine.class);
        StoryMemory memory = new StoryMemory(5);

        StoryResponse firstResponse = new StoryResponse(
                "You found a silver key.",
                "Where do you go next?",
                List.of("Open the door", "Inspect the key", "Go back"),
                new Player("P", 100, 10),
                false
        );

        StoryResponse secondResponse = new StoryResponse(
                "You use the silver key to open the old door.",
                "What do you do inside?",
                List.of("Enter", "Listen", "Run away"),
                new Player("P", 100, 10),
                false
        );

        when(delegate.request(any()))
                .thenReturn(firstResponse)
                .thenReturn(secondResponse);

        MemoryStoryEngineDec engine = new MemoryStoryEngineDec(delegate, memory);

        StoryPrompt firstPrompt = mock(StoryPrompt.class);
        when(firstPrompt.toPromptString()).thenReturn("Start story");

        StoryPrompt secondPrompt = mock(StoryPrompt.class);
        when(secondPrompt.toPromptString()).thenReturn("Continue story");

        engine.request(firstPrompt);
        engine.request(secondPrompt);

        ArgumentCaptor<StoryPrompt> captor = ArgumentCaptor.forClass(StoryPrompt.class);
        verify(delegate, times(2)).request(captor.capture());

        StoryPrompt secondDecoratedPrompt = captor.getAllValues().get(1);
        String secondPromptText = secondDecoratedPrompt.toPromptString();

        assertTrue(secondPromptText.contains("You found a silver key."));
        assertTrue(secondPromptText.contains("Where do you go next?"));
        assertTrue(secondPromptText.contains("Continue story"));
    }
    @Test
    void shouldRemoveOldestBeatWhenMaxBeatsIsExceeded() {
        StoryMemory memory = new StoryMemory(2);

        StoryResponse first = new StoryResponse(
                "First event: player entered the forest.",
                "Question 1?",
                List.of("A", "B", "C"),
                new Player("P", 100, 10),
                false
        );

        StoryResponse second = new StoryResponse(
                "Second event: player found a sword.",
                "Question 2?",
                List.of("A", "B", "C"),
                new Player("P", 100, 15),
                false
        );

        StoryResponse third = new StoryResponse(
                "Third event: player met a dragon.",
                "Question 3?",
                List.of("A", "B", "C"),
                new Player("P", 80, 15),
                false
        );

        memory.remember(first);
        memory.remember(second);
        memory.remember(third);

        String summary = memory.summary();

        assertFalse(summary.contains("First event"));
        assertTrue(summary.contains("Second event"));
        assertTrue(summary.contains("Third event"));
    }
    @Test
    void shouldReturnEmptyMemoryMessageWhenNoBeatsExist() {
        StoryMemory memory = new StoryMemory(2);

        assertEquals("No previous story events.", memory.summary());
    }
    @Test
    void shouldNotRememberFailedResponses() {
        StoryEngine delegate = mock(StoryEngine.class);
        StoryMemory memory = new StoryMemory(5);

        when(delegate.request(any()))
                .thenThrow(new IllegalStateException("LLM failed"));

        MemoryStoryEngineDec engine = new MemoryStoryEngineDec(delegate, memory);

        StoryPrompt prompt = mock(StoryPrompt.class);
        when(prompt.toPromptString()).thenReturn("Start story");

        assertThrows(IllegalStateException.class, () -> engine.request(prompt));

        assertEquals("No previous story events.", memory.summary());
    }
    @Test
    void shouldMeasureContextLengthAndTokenCost() {
        StoryMemory memory = new StoryMemory(3);
        StoryResponse first = new StoryResponse(
                "First event: player entered the forest.",
                "Question 1?",
                List.of("A", "B", "C"),
                new Player("P", 100, 10),
                false
        );

        StoryResponse second = new StoryResponse(
                "Second event: player found a sword.",
                "Question 2?",
                List.of("A", "B", "C"),
                new Player("P", 100, 15),
                false
        );
        memory.remember(first);
        memory.remember(second);

        String context = memory.context();
        assertFalse(context.isEmpty());
        assertEquals(context.length() / 4, memory.approximateTokenCost());
    }
}
