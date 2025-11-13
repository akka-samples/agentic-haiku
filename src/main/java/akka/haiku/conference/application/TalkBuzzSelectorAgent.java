package akka.haiku.conference.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(id = "buzz-selector-agent")
public class TalkBuzzSelectorAgent extends Agent {


  private static final Logger log = LoggerFactory.getLogger(TalkBuzzSelectorAgent.class);
  private final String SYS_MESSAGE = """
    You are responsible for selecting technical jargon from the user input.
    You should analyze the text and return 5 to 7 words that can be considered a technical world or a tech jargon.
    You return the selected words as text.
    Watch-out for framework and library names. If there is any framework or library name in the text, they should be included in your selection.
    """;

  public Effect<String> extraBuzzWords(String talkDescription) {
    return effects()
      .systemMessage(SYS_MESSAGE)
      .memory(MemoryProvider.none())
      .userMessage(talkDescription)
      .thenReply();
  }

}
