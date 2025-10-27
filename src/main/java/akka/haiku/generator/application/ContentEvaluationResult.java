package akka.haiku.generator.application;

import akka.javasdk.agent.EvaluationResult;

public record ContentEvaluationResult(String explanation, boolean passed) implements EvaluationResult {

  public static ContentEvaluationResult passedResult() {
    return new ContentEvaluationResult("Content is ok.", true);
  }

  public static ContentEvaluationResult notPassedResult(String explanation) {
    return new ContentEvaluationResult(explanation, false);
  }
}
