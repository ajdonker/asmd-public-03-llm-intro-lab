package e1.prompt;

import e1.model.Player;

public record BeginPrompt(Player player, String setting) implements StoryPrompt {
    private static final String TEMPLATE = """
            You are a text adventure game engine.

            Start a new story for the player.

            Player:
            {
              "name": "%s",
              "health": %d,
              "attackPower": %d
            }

            Initial setting:
            %s

            Your task:
            - Write a short narrative introducing the story.
            - Ask the player what they want to do next.
            - Give exactly 3 possible choices.
            - Update the player state if needed.
            - Set gameOver to true if the story ends, otherwise false.

            Return ONLY a valid JSON object.
            Do not use Markdown.
            Do not wrap the JSON in ```json.
            Do not add any explanation before or after the JSON.

            The JSON must have exactly this structure:
            {
              "narrative": "short story text",
              "question": "question for the player",
              "choices": ["choice 1", "choice 2", "choice 3"],
              "updatedPlayer": {
                "name": "%s",
                "health": %d,
                "attackPower": %d
              },
              "gameOver": false
            }
            """; 


    @Override
    public String toPromptString() {
        return TEMPLATE.formatted(player.name(),
                player.health(),
                player.attackPower(),
                setting,
                player.name(),
                player.health(),
                player.attackPower());
    }
}
