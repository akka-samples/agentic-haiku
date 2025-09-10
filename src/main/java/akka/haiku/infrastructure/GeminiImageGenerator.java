package akka.haiku.infrastructure;

import akka.haiku.application.ImageGenerator;

public class GeminiImageGenerator implements ImageGenerator {

  @Override
  public String generateImage(String prompt) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Manta_birostris-Thailand.jpg/640px-Manta_birostris-Thailand.jpg";
  }
}
