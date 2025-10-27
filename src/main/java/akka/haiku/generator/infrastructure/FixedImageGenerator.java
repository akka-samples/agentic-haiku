package akka.haiku.generator.infrastructure;

import akka.haiku.generator.application.ImageGenerator;


public class FixedImageGenerator implements ImageGenerator {

  @Override
  public String generateImage(String userInput, String haiku) {
    return "static/img/local-image.png";
  }
}
