package akka.haiku.generator.domain;

import java.time.Instant;
import java.util.Optional;

public record ContentGeneration(String userInput, Long generatedAt, Optional<Haiku> haiku, Optional<Image> image) {

  public static ContentGeneration empty() {
    return new ContentGeneration("***********", null, Optional.empty(), Optional.empty());
  }

  public ContentGeneration withImagePrompt(String userInput) {
    return new ContentGeneration(userInput, generatedAt, haiku, image);
  }

  public ContentGeneration withImageUrl(String imageUrl) {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), haiku, Optional.of(new Image(imageUrl)));
  }

  public ContentGeneration withUserInput(String userInput) {
    return new ContentGeneration(userInput, generatedAt, haiku, image) ;
  }

  public ContentGeneration withHaiku(Haiku haiku) {
    return new ContentGeneration(userInput, generatedAt, Optional.of(haiku), image);
  }
}
