package akka.haiku.generator.domain;

import java.time.Instant;
import java.util.Optional;

import static akka.haiku.generator.domain.ContentGenerationStatus.ACCEPTED;
import static akka.haiku.generator.domain.ContentGenerationStatus.HAIKU_GENERATED;
import static akka.haiku.generator.domain.ContentGenerationStatus.IMAGE_GENERATED;
import static akka.haiku.generator.domain.ContentGenerationStatus.NEGATIVITY_DETECTED;
import static akka.haiku.generator.domain.ContentGenerationStatus.STARTED;
import static akka.haiku.generator.domain.ContentGenerationStatus.TIMED_OUT;
import static akka.haiku.generator.domain.ContentGenerationStatus.TOXICITY_DETECTED;
import static akka.haiku.generator.domain.ContentGenerationStatus.VALIDATED;

public record ContentGeneration(String userInput,
                                Long generatedAt,
                                ContentGenerationStatus status,
                                Optional<Haiku> haiku,
                                Optional<Image> image) {

  public static ContentGeneration empty() {
    return new ContentGeneration("***********", null, STARTED, Optional.empty(), Optional.empty());
  }

  public ContentGeneration withImageUrl(String imageUrl) {
    return new ContentGeneration(userInput, generatedAt, IMAGE_GENERATED, haiku, Optional.of(new Image(imageUrl)));
  }

  public ContentGeneration withUserInput(String userInput) {
    return new ContentGeneration(userInput, generatedAt, status, haiku, image) ;
  }

  public ContentGeneration withHaiku(Haiku haiku) {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), HAIKU_GENERATED, Optional.of(haiku), image);
  }

  public boolean isComplete() {
    return image.isPresent();
  }

  public ContentGeneration accepted() {
    return new ContentGeneration(userInput, generatedAt, ACCEPTED, haiku, image);
  }

  public ContentGeneration toxicityDetected() {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), TOXICITY_DETECTED, haiku, Optional.of(new Image("static/img/censored.png")));
  }

  public ContentGeneration negativityDetected() {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), NEGATIVITY_DETECTED, haiku, Optional.of(new Image("static/img/censored.png")));
  }

  public ContentGeneration validated() {
    return new ContentGeneration(userInput, generatedAt, VALIDATED, haiku, image);
  }

  public ContentGeneration timedOut() {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), TIMED_OUT, haiku, Optional.of(new Image("static/img/time-is-up.png")));
  }
}
