package akka.haiku.gateway.application;

import akka.haiku.gateway.domain.TokenGroup;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static akka.haiku.Bootstrap.TOKEN_GROUP_SIZE;

@ComponentId("token-group-consumer")
@Consume.FromKeyValueEntity(TokenGroupEntity.class)
public class TokenGroupConsumer extends Consumer {

  private static final Logger log = LoggerFactory.getLogger(TokenGroupConsumer.class);
  private final ComponentClient componentClient;

  public TokenGroupConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onChange(TokenGroup tokenGroup) {
    if (tokenGroup.hasAvailableTokens()){
      return effects().ignore();
    } else {
      String tokenGroupId = UUID.randomUUID().toString();
      log.info("Token group {} has no available tokens, generating new group {}", tokenGroup.groupId(), tokenGroupId);
      componentClient.forKeyValueEntity(tokenGroupId)
        .method(TokenGroupEntity::create)
        .invokeAsync(TOKEN_GROUP_SIZE);

      return effects().done();
    }
  }
}
