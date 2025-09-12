package akka.haiku.gateway.domain;

import java.util.List;
import java.util.Optional;

public record TokenGroup(String groupId, List<Token> tokens) {

  public static TokenGroup create(String groupId, int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Size must be greater than 0");
    } else {
      List<Token> tokens = java.util.stream.IntStream.range(0, size)
          .mapToObj(i -> new Token(java.util.UUID.randomUUID().toString(), TokenStatus.AVAILABLE))
          .toList();
      return new TokenGroup(groupId,tokens);
    }
  }

  public Optional<Token> getFirstAvailable() {
    return tokens.stream().filter(token -> token.status() == TokenStatus.AVAILABLE).findFirst();
  }

  public TokenGroup reserveToken(Token token) {
    tokens.stream()
      .filter(t -> t.value().equals(token.value()))
      .forEach(Token::reserve);
    return this;
  }

  public boolean isValid(String token) {
    return tokens.stream().anyMatch(t -> t.isValid(token));
  }

  public TokenGroup markAsUsed(String token) {
    tokens.stream()
      .filter(t -> t.value().equals(token))
      .forEach(Token::use);
    return this;
  }

  public long availableTokens() {
    return tokens.stream().filter(t -> t.status() == TokenStatus.AVAILABLE).count();
  }

  public boolean hasAvailableTokens() {
    return tokens.stream().anyMatch(t -> t.status() == TokenStatus.AVAILABLE);
  }
}
