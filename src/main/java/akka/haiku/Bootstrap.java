package akka.haiku;

import akka.haiku.gateway.application.QrCodeGenerator;
import akka.haiku.gateway.application.TokenGroupEntity;
import akka.haiku.generator.application.ImageGenerator;
import akka.haiku.generator.infrastructure.FixedImageGenerator;
import akka.haiku.generator.infrastructure.GeminiImageGenerator;
import akka.haiku.storage.infrastructure.GCPBlobStorage;
import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.typesafe.config.Config;

import java.util.UUID;

@Setup
public class Bootstrap implements ServiceSetup {

  private final ComponentClient componentClient;
  private final int tokenGroupSize;
  private final Config config;

  public Bootstrap(Config config, ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.config = config;
    this.tokenGroupSize = config.getInt("haiku.app.token-group-size");
  }

  @Override
  public void onStartup() {
    componentClient.forKeyValueEntity(UUID.randomUUID().toString())
      .method(TokenGroupEntity::create)
      .invokeAsync(tokenGroupSize);
  }

  @Override
  public DependencyProvider createDependencyProvider() {
    var blobStorage = new GCPBlobStorage();
    var imageGenerator = new GeminiImageGenerator(blobStorage);
    var fixedGenerator = new FixedImageGenerator();
    var qrCodeGenerator = new QrCodeGenerator(blobStorage, config);

    return new DependencyProvider() { // <3>
      @Override
      public <T> T getDependency(Class<T> clazz) {
        if (clazz == ImageGenerator.class) {
          var googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
          if (googleApplicationCredentials == null || googleApplicationCredentials.isEmpty()) {
            return (T) fixedGenerator;
          } else {
            return (T) imageGenerator;
          }
        } else if (clazz == QrCodeGenerator.class) {
          return (T) qrCodeGenerator;
        } else {
          throw new RuntimeException("No such dependency found: " + clazz);
        }
      }
    };
  }
}
