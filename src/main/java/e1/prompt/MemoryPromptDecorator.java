package e1.prompt;

public record MemoryPromptDecorator (
    StoryPrompt originalPrompt,
    String memorySummary
) implements StoryPrompt {
    @Override
    public String toPromptString() {
        return """
                Important story memory:
                %s

                Use this memory to keep the story coherent.
                Do not contradict previous events, player state, or important story facts.

                Current request:
                %s
                """.formatted(memorySummary, originalPrompt.toPromptString());
    }
}
