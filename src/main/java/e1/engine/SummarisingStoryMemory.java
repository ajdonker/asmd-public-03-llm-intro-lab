package e1.engine;

import dev.langchain4j.model.chat.ChatModel;
import e1.model.Story;
import e1.model.StoryResponse;
import e1.prompt.StoryPrompt;

public class SummarisingStoryMemory implements StoryContextManager {
    private final ChatModel model;
    private String summary = "No previous story events.";
    private int calls = 0;
    public SummarisingStoryMemory(ChatModel model) {
        this.model = model;
    }
    @Override
    public String context() {
        return summary;
    }

    @Override
    public void remember(StoryPrompt prompt, StoryResponse response) {
        String summarisePrompt = """
                You are maintaining memory for a text adventure game.

                Existing memory summary:
                %s

                New story beat:
                Narrative: %s
                Question: %s
                Updated player: %s
                Game over: %s

                Produce an updated concise summary.
                Keep important characters, locations, goals, inventory, dangers, and unresolved events.
                Do not invent new facts.
                Return only the summary text.
                """.formatted(summary, response.narrative(), response.question(), response.updatedPlayer(), response.gameOver());

        summary = model.chat(summarisePrompt);
        calls++;
    }
    public int getCalls() {
        return this.calls;
    }
}
