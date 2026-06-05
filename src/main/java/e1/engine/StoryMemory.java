package e1.engine;

import e1.model.StoryResponse;
import e1.prompt.StoryPrompt;

import java.util.ArrayList;
import java.util.List;

public class StoryMemory implements StoryContextManager {
    private final int maxBeats;
    private final List<String> beats = new ArrayList<>();

    public StoryMemory(int maxBeats) {
        this.maxBeats = maxBeats;
    }

    public String summary() {
        if(beats.isEmpty()) {
            return "No previous story events.";
        }
        return String.join("\n", beats);
    }
    @Override
    public String context() {
        return summary();
    }
    public void remember(StoryResponse response) {
        String beat = """
                - Narrative: %s
                  Question: %s
                  Player: %s
                  Game over: %s
                """.formatted(response.narrative(),
                response.question(),
                response.updatedPlayer(),
                response.gameOver());

        beats.add(beat);

        while(beats.size() > maxBeats) {
            beats.removeFirst(); //remove oldest beats in list to manage memory
        }
    }
    @Override
    public void remember(StoryPrompt prompt,StoryResponse response) {
        remember(response);
    }
}
