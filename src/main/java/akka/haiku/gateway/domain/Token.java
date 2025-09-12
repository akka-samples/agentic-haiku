package akka.haiku.gateway.domain;

//not a record because status is mutable
public class Token {
  private final String value;
  private TokenStatus status;

  public Token(String value, TokenStatus status) {
    this.value = value;
    this.status = status;
  }

  public String value() {
    return value;
  }

  public TokenStatus status() {
    return status;
  }

  public void reserve() {
    status = TokenStatus.RESERVED;
  }

  public void use() {
    status = TokenStatus.USED;
  }

  public boolean isValid(String token) {
    return token.equals(value) && status == TokenStatus.RESERVED;
  }
}
