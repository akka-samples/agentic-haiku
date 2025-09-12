package akka.haiku.generator.domain;

public record UserInput(String originalInput, Eval eval) {


  public static UserInput of(String originalInput) {
    return new UserInput(originalInput, Eval.UNKNOWN);
  }

  public UserInput evaluatedAs(Eval eval) {
    return new UserInput(originalInput, eval);
  }

  public boolean isRejected() {
    return eval == Eval.TOXIC_CONTENT || eval == Eval.NEGATIVE;
  }

  public boolean isAccepted() {
    return eval == Eval.NEUTRAL || eval == Eval.POSITIVE || eval == Eval.NON_TOXIC_CONTENT;
  }

  public boolean isNegative() {
    return eval == Eval.NEGATIVE;
  }

  public enum Eval {
    UNKNOWN,
    TOXIC_CONTENT,
    NON_TOXIC_CONTENT,
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
  }

}
