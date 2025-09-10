package akka.haiku.domain;

public record UserInput(String originalInput, Eval eval) {


  public static UserInput of(String originalInput) {
    return new UserInput(originalInput, Eval.UNKNOWN);
  }

  public UserInput evaluatedAs(Eval eval) {
    return new UserInput(originalInput, eval);
  }

  public boolean isRejected() {
    return eval == Eval.HARMFUL_CONTENT || eval == Eval.NEGATIVE;
  }

  public boolean isAccepted() {
    return eval == Eval.NEUTRAL || eval == Eval.POSITIVE || eval == Eval.NOT_HARMFUL_CONTENT;
  }

  public boolean isNegative() {
    return eval == Eval.NEGATIVE;
  }

  public enum Eval {
    UNKNOWN,
    HARMFUL_CONTENT,
    NOT_HARMFUL_CONTENT,
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
  }

}
