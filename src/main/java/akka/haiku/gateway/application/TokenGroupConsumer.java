package akka.haiku.gateway.application;

import akka.haiku.gateway.domain.TokenGroup;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ComponentId("token-group-consumer")
@Consume.FromKeyValueEntity(TokenGroupEntity.class)
public class TokenGroupConsumer extends Consumer {

  private static final Logger log = LoggerFactory.getLogger(TokenGroupConsumer.class);
  private final ComponentClient componentClient;
  private final QrCodeGenerator qrCodeGenerator;
  private final int tokenGroupSize;

  public TokenGroupConsumer(ComponentClient componentClient, QrCodeGenerator qrCodeGenerator, Config config) {
    this.componentClient = componentClient;
    this.qrCodeGenerator = qrCodeGenerator;
    this.tokenGroupSize = config.getInt("haiku.app.token-group-size");
  }

  public Effect onChange(TokenGroup tokenGroup) {

    if (tokenGroup.isNewlyCreated()) {
      log.info("New token group created {}, generating new qr code", tokenGroup.groupId());
      log.info("Go to form url: {}", "http://localhost:9000/gateway/" + tokenGroup.groupId());
      var qrCodeUrl = qrCodeGenerator.generate(tokenGroup.groupId());
      componentClient.forKeyValueEntity(tokenGroup.groupId())
        .method(QrCodeEntity::create)
        .invokeAsync(qrCodeUrl);
      return effects().done();

    } else if (tokenGroup.isNearExhaustion()) {
      String tokenGroupId = UUID.randomUUID().toString();
      log.debug("Token group {} is almost exhausted, generating new group {}", tokenGroup.groupId(), tokenGroupId);
      componentClient.forKeyValueEntity(tokenGroupId)
        .method(TokenGroupEntity::create)
        .invokeAsync(tokenGroupSize);
      return effects().done();

    } else {
      return effects().ignore();
    }
  }
}
