package akka.haiku.generator.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.haiku.generator.application.ContentEvaluationResult.notPassedResult;
import static akka.haiku.generator.application.ContentEvaluationResult.passedResult;

@Component(id = "sentiment-analysis")
public class SentimentDetectorAgent extends Agent {


  private static final Logger log = LoggerFactory.getLogger(SentimentDetectorAgent.class);
  private final String SYS_MESSAGE = """
    You are responsible for detecting whether an input message is positive, neutral or negative. 
    Analyze the input carefully. Respond with one of the following:
    
    - POSITIVE if the message contains a positive tone
    - NEUTRAL if the message contains a neutral tone
    - NEGATIVE if the message contains a negative tone
    
    If in doubt, always respond with UNKNOWN. 
    Do not provide explanations or additional text—only respond with one of the three options above.
    """;

  public Effect<ContentEvaluationResult> analyseSentiment(String input) {
    return effects()
      .systemMessage(SYS_MESSAGE)
      // we don't want to save any content before we are able to decide if it's harmful or not
      .memory(MemoryProvider.none())
      .userMessage(input)
      .map(res -> {
        log.info("Sentiment detector agent analyse content: {}", res);
        if ("NEGATIVE".equals(res)) {
          return notPassedResult("Content is evaluated as: " + res);
        }
        return passedResult();
      })
      .thenReply();
  }

}
