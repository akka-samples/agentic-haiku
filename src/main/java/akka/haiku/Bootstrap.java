package akka.haiku;

import akka.haiku.generator.application.ImageGenerator;
import akka.haiku.generator.infrastructure.FixedImageGenerator;
import akka.haiku.generator.infrastructure.GeminiImageGenerator;
import akka.haiku.storage.application.BlobStorage;
import akka.haiku.storage.infrastructure.GCPBlobStorage;
import akka.haiku.storage.infrastructure.LocalStorage;
import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setup
public class Bootstrap implements ServiceSetup {

  private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

  @Override
  public DependencyProvider createDependencyProvider() {
    var blobStorage = createBlobStorage();
    var imageGenerator = createImageGenerator(blobStorage);

    return new DependencyProvider() { // <3>
      @Override
      public <T> T getDependency(Class<T> clazz) {
        if (clazz == ImageGenerator.class) {
          return (T) imageGenerator;
        } else {
          throw new RuntimeException("No such dependency found: " + clazz);
        }
      }
    };
  }

  private BlobStorage createBlobStorage() {
    var googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    if (googleApplicationCredentials == null || googleApplicationCredentials.isEmpty()) {
      log.info("Using LocalStorage for blobs");
      return new LocalStorage();
    } else {
      log.info("Using GCPBlobStorage for blobs");
      return new GCPBlobStorage();
    }
  }

  private ImageGenerator createImageGenerator(BlobStorage blobStorage) {
    var googleApplicationCredentials = System.getenv("GOOGLE_AI_GEMINI_API_KEY");
    if (googleGeminiCredentials == null || googleGeminiCredentials.isEmpty()) {
      log.info("Using FixedImageGenerator for images");
      return new FixedImageGenerator();
    } else {
      log.info("Using GeminiImageGenerator for images");
      return new GeminiImageGenerator(blobStorage);
    }
  }
}
