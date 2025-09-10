package akka.haiku.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.ComponentId;

import java.util.List;

@ComponentId("prompt-agent")
public class PromptAgent extends Agent {

  public record PromptInputs(List<String> inputs) {
  }

  public Effect<String> composePrompt(PromptInputs promptInputs) {
    return effects()
      .memory(MemoryProvider.none())
      .systemMessage("You are a prompt generation agent. Generate a prompt to create an image based on the following random inputs. Try to refer to all of them. " +
        "The selected style should be a modern digital art style, vibrant and colorful. " +
        "Here are the inputs separated by new lines: ")
      .userMessage(String.join("\n", promptInputs.inputs()))
      .thenReply();
  }
}
