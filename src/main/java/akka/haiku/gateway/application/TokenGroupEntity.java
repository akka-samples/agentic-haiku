package akka.haiku.gateway.application;

import akka.Done;
import akka.haiku.gateway.domain.TokenGroup;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import akka.javasdk.keyvalueentity.KeyValueEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;

@ComponentId("token-group")
public class TokenGroupEntity extends KeyValueEntity<TokenGroup> {

  private static final Logger log = LoggerFactory.getLogger(TokenGroupEntity.class);
  private final String groupId;

  public TokenGroupEntity(KeyValueEntityContext keyValueEntityContext) {
    this.groupId = keyValueEntityContext.entityId();
  }

  public Effect<Done> create(int size) {
    log.info("Creating token group with {} tokens", size);
    if (currentState() != null) {
      return effects().reply(done());
    } else {
      TokenGroup tokenGroup = TokenGroup.create(groupId, size);
      return effects()
        .updateState(tokenGroup)
        .thenReply(done());
    }
  }

  public Effect<String> getToken() {
    TokenGroup tokenGroup = currentState();
    if (tokenGroup == null) {
      return effects().error("Token group not found " + groupId);
    } else {
      return tokenGroup.getFirstAvailable()
        .map(token -> {
          TokenGroup updatedGroup = tokenGroup.reserveToken(token);
          log.debug("Reserving token {}, available tokes {}", token.value(), updatedGroup.availableTokens());
          return effects()
            .updateState(updatedGroup)
            .thenReply(token.value());
        })
        .orElseGet(() -> effects().error("No available tokens for " + groupId));
    }
  }

  public Effect<Boolean> isValid(String token) {
    TokenGroup tokenGroup = currentState();
    if (tokenGroup == null) {
      return effects().reply(false);
    } else {
      log.debug("Validating token {} from group {}", token, groupId);
      return effects().reply(tokenGroup.isValid(token));
    }
  }

  public Effect<Done> tokenUsed(String token) {
    TokenGroup tokenGroup = currentState();
    if (tokenGroup == null) {
      return effects().error("Token group not found " + groupId);
    } else {
      log.debug("Marking token {} as used from group {}", token, groupId);
      return effects()
        .updateState(tokenGroup.markAsUsed(token))
        .thenReply(done());
    }
  }
}
