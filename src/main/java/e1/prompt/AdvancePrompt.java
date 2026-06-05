package e1.prompt;

import e1.model.Player;

public record AdvancePrompt(
    Player player,
    String previousQuestion,
    String chosenAction
) implements StoryPrompt {

    private static final String TEMPLATE =  """
            You are a text adventure game engine.

            Continue the story based on the player's chosen action.

            Current player:
            {
              "name": "%s",
              "health": %d,
              "attackPower": %d
            }

            Previous question:
            %s

            Player's chosen action:
            %s

            Your task:
            - Continue the narrative based on the chosen action.
            - Describe the consequence of the action.
            - Ask the next question.
            - Give exactly 3 choices.
            - Update the player state if needed.
            - Set gameOver to true if the player dies, wins, or the story ends.
            - Otherwise set gameOver to false.

            Return ONLY a valid JSON object.
            Do not use Markdown.
            Do not wrap the JSON in ```json.
            Do not add any explanation before or after the JSON.

            The JSON must have exactly this structure:
            {
              "narrative": "what happens next",
              "question": "what should the player do next?",
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
        return TEMPLATE.formatted(player.name(), player.health(), player.attackPower(),previousQuestion,chosenAction,
                player.name(),player.health(),player.attackPower());
    }
}
