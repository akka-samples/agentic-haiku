package akka.haiku;

import akka.haiku.conference.api.ScheduleEndpoint;
import akka.haiku.conference.application.ScheduleScanner;
import akka.haiku.gateway.application.QrCodeGenerator;
import akka.haiku.gateway.application.TokenGroupEntity;
import akka.haiku.generator.application.ImageGenerator;
import akka.haiku.generator.infrastructure.FixedImageGenerator;
import akka.haiku.generator.infrastructure.GeminiImageGenerator;
import akka.haiku.storage.application.BlobStorage;
import akka.haiku.storage.infrastructure.GCPBlobStorage;
import akka.haiku.storage.infrastructure.LocalStorage;
import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpClient;
import akka.javasdk.http.HttpClientProvider;
import akka.javasdk.timer.TimerScheduler;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Setup
public class Bootstrap implements ServiceSetup {

  private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
  private final ComponentClient componentClient;
  private final int tokenGroupSize;
  private final Config config;
  private final HttpClient httpClient;
  private final TimerScheduler timerScheduler;

  public Bootstrap(Config config,
                   ComponentClient componentClient,
                   HttpClientProvider httpClientProvider,
                   TimerScheduler timerScheduler

  ) {

    this.httpClient = httpClientProvider.httpClientFor("https://dvbe25.cfp.dev");
    this.timerScheduler = timerScheduler;
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
    var blobStorage = createBlobStorage();
    var imageGenerator = createImageGenerator(blobStorage);
    var qrCodeGenerator = new QrCodeGenerator(blobStorage, config);
    var scanner = new ScheduleScanner(httpClient, timerScheduler, componentClient);

    return new DependencyProvider() { // <3>
      @Override
      public <T> T getDependency(Class<T> clazz) {
        if (clazz == ScheduleScanner.class) {
          return (T) scanner;
        } else if (clazz == ImageGenerator.class) {
          return (T) imageGenerator;
        } else if (clazz == QrCodeGenerator.class) {
          return (T) qrCodeGenerator;
        } else {
          throw new RuntimeException("No such dependency found: " + clazz);
        }
      }
    };
  }

  private BlobStorage createBlobStorage() {
    var googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    if (googleApplicationCredentials == null || googleApplicationCredentials.isEmpty()) {
      log.debug("Using LocalStorage for blobs");
      return new LocalStorage();
    } else {
      return new GCPBlobStorage();
    }
  }

  private ImageGenerator createImageGenerator(BlobStorage blobStorage) {
    var googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    if (googleApplicationCredentials == null || googleApplicationCredentials.isEmpty()) {
      log.debug("Using FixedImageGenerator for images");
      return new FixedImageGenerator();
    } else {
      return new GeminiImageGenerator(blobStorage);
    }
  }
}
