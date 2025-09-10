package akka.haiku.application;

import akka.haiku.domain.UserInput;
import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.ComponentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("harmful-content-detector")
public class HarmfulContentDetectorAgent extends Agent {


  private static final Logger log = LoggerFactory.getLogger(HarmfulContentDetectorAgent.class);
  private final String SYS_MESSAGE = """
    You are responsible for detecting whether an input message contains any harmful, offensive, or inappropriate language. 
    Analyze the input carefully. Respond with one of the following:
    
    - HARMFUL_CONTENT if the message contains harmful or inappropriate language,
    - NOT_HARMFUL_CONTENT if the message is safe,
    - UNKNOWN if you are unsure or cannot determine.
    
    If in doubt, always respond with UNKNOWN. 
    Do not provide explanations or additional text—only respond with one of the three options above.
    """;

  public Effect<UserInput> evaluate(UserInput input) {
    return effects()
      .systemMessage(SYS_MESSAGE)
      // we don't want to save any content before we are able to decide if it's harmful or not
      .memory(MemoryProvider.none())
      .userMessage(input.originalInput())
      .map( res -> {
        UserInput.Eval eval;
        log.debug("evaluation result is '{}'", res);
        try {
          eval = UserInput.Eval.valueOf(res);
        } catch (IllegalArgumentException e) {
          eval = UserInput.Eval.UNKNOWN;
        }
        return input.evaluatedAs(eval);
      })
      .thenReply();
  }

}
