package akka.haiku.generator.application;

import akka.haiku.generator.domain.Haiku;
import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.ComponentId;

@ComponentId("haiku-generator")
public class HaikuGenAgent extends Agent {

  private final String SYS_MESSAGE = """
    Write a haiku inspired by the following input. A haiku is a three-line poem with a 5-7-5 syllable structure.
    Focus on capturing the essence, mood, or imagery of the input in a concise and poetic way.
    """;

  public Effect<Haiku> generate(String input) {
    return effects()
      .systemMessage(SYS_MESSAGE)
      .userMessage(input)
      .responseConformsTo(Haiku.class)
      .thenReply();
  }
}
