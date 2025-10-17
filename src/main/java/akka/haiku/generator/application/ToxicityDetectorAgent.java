package akka.haiku.generator.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.haiku.generator.application.ContentEvaluationResult.notPassedResult;
import static akka.haiku.generator.application.ContentEvaluationResult.passedResult;

@Component(id = "toxic-content-detector")
public class ToxicityDetectorAgent extends Agent {


  private static final Logger log = LoggerFactory.getLogger(ToxicityDetectorAgent.class);
  private final String SYS_MESSAGE = """
    Your job is to judge whether a message contains toxic content. 
    Toxic content includes, but is not limited to: hateful remarks, insults, degrading language, inappropriate words, 
    or any threats of harm (physical or emotional).
    
    Be strict: if a message fits this description, do not hesitate to flag it.
    
    Respond with one of these options only:
    - TOXIC_CONTENT: if the message is toxic as described above
    - NON_TOXIC_CONTENT: if the message is free of toxic content
    - UNKNOWN: if you are unsure or cannot decide
    
    If you are uncertain, always choose UNKNOWN. Do not explain your answer or add any extra text. Only reply with one of the three options above.
    
    """;

  public Effect<ContentEvaluationResult> evaluateContent(String input) {
    return effects()
      .systemMessage(SYS_MESSAGE)
      // we don't want to save any content before we are able to decide if it's harmful or not
      .memory(MemoryProvider.none())
      .userMessage(input)
      .map(res -> {
        log.info("Toxicity detector agent evaluate content: {}", res);
        if ("NON_TOXIC_CONTENT".equals(res)) {
          return passedResult();
        }
        return notPassedResult("Content is evaluated as: " + res);
      })
      .thenReply();
  }

}
