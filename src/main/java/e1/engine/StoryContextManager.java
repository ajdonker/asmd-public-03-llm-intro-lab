package e1.engine;

import e1.model.StoryResponse;
import e1.prompt.StoryPrompt;

public interface StoryContextManager {
    String context();

    void remember(StoryPrompt prompt, StoryResponse response);

    default int approximateTokenCost() {
        return context().length() / 4;
        // a metric so we can compare both strategies in terms of cost 
    }
}
