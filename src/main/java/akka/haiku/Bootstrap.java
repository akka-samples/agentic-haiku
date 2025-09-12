package akka.haiku;

import akka.haiku.gateway.application.TokenGroupEntity;
import akka.haiku.generator.infrastructure.FixedImageGenerator;
import akka.haiku.generator.infrastructure.GeminiImageGenerator;
import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.typesafe.config.Config;

@Setup
public class Bootstrap implements ServiceSetup {

  public static final int TOKEN_GROUP_SIZE = 2;
  private final ComponentClient componentClient;

  public Bootstrap(Config config, ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public void onStartup() {
    //TODO create first token group on startup, this should trigger QE code generation
    componentClient.forKeyValueEntity("1")
      .method(TokenGroupEntity::create)
      .invokeAsync(TOKEN_GROUP_SIZE);
  }

  @Override
  public DependencyProvider createDependencyProvider() {
    return DependencyProvider.single(new GeminiImageGenerator());
  }
}
