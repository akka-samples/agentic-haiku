package akka.haiku.gateway.domain;

import java.util.List;
import java.util.Optional;

public record TokenGroup(List<Token> tokens) {

  public static TokenGroup create(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Size must be greater than 0");
    } else {
      List<Token> tokens = java.util.stream.IntStream.range(0, size)
          .mapToObj(i -> new Token(java.util.UUID.randomUUID().toString(), TokenStatus.AVAILABLE))
          .toList();
      return new TokenGroup(tokens);
    }
  }

  public Optional<Token> getFirstAvailable() {
    return tokens.stream().filter(token -> token.status() == TokenStatus.AVAILABLE).findFirst();
  }

  public TokenGroup reserveToken(Token token) {
    tokens.stream()
      .filter(t -> t.value().equals(token.value()))
      .forEach(Token::setInUse);
    return this;
  }

  public boolean isValid(String token) {
    return tokens.stream().anyMatch(t -> t.isValid(token));
  }

  public TokenGroup markAsUsed(String token) {
    tokens.stream()
      .filter(t -> t.value().equals(token))
      .forEach(Token::setUsed);
    return this;
  }
}
