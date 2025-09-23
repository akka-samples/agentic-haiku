package akka.haiku.generator.infrastructure;

import akka.haiku.generator.application.ImageGenerator;


public class FixedImageGenerator implements ImageGenerator {

  @Override
  public String generateImage(String userInput, String haiku) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Manta_birostris-Thailand.jpg/640px-Manta_birostris-Thailand.jpg";
  }
}
