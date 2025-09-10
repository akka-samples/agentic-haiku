package akka.haiku.domain;

import java.time.Instant;
import java.util.Optional;

public record ContentGeneration(String inputId, String prompt, Long generatedAt, Optional<Haiku> haiku, Optional<Image> image) {

  public static ContentGeneration of(String inputId, String input) {
    return new ContentGeneration(inputId, input, null,  Optional.empty(), Optional.empty());
  }

  public ContentGeneration withImagePrompt(String prompt) {
    return new ContentGeneration(inputId, prompt, generatedAt, haiku, image);
  }

  public ContentGeneration withImageUrl(String imageUrl) {
    return new ContentGeneration(inputId, prompt, Instant.now().toEpochMilli(), haiku, Optional.of(new Image(imageUrl)));
  }
}
