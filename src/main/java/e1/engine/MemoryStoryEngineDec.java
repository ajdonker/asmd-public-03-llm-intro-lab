package e1.engine;

import e1.model.StoryResponse;
import e1.prompt.MemoryPromptDecorator;
import e1.prompt.StoryPrompt;

public class MemoryStoryEngineDec implements StoryEngine {
    private final StoryEngine delegate;
    //private final StoryMemory memory;
    private final StoryContextManager contextManager;
    public MemoryStoryEngineDec(StoryEngine delegate, StoryContextManager contextManager) {
        this.delegate = delegate;
        this.contextManager = contextManager;
    }

    @Override
    public StoryResponse request(StoryPrompt prompt) {
        StoryPrompt decoratedPrompt = new MemoryPromptDecorator(prompt, contextManager.context());

        StoryResponse response = delegate.request(decoratedPrompt);

        contextManager.remember(prompt, response);
        return response;
    }
    public int approximateTokenCost() {
        return contextManager.approximateTokenCost();
    }
}
