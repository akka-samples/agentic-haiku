package akka.haiku.generator.domain;

import java.time.Instant;
import java.util.Optional;

import static akka.haiku.generator.domain.HaikuGenerationStatus.ACCEPTED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.HAIKU_GENERATED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.IMAGE_GENERATED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.NEGATIVITY_DETECTED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.STARTED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.TIMED_OUT;
import static akka.haiku.generator.domain.HaikuGenerationStatus.TOXICITY_DETECTED;
import static akka.haiku.generator.domain.HaikuGenerationStatus.VALIDATED;

public record HaikuGeneration(HaikuId haikuId,
                              String userInput,
                              Long generatedAt,
                              HaikuGenerationStatus status,
                              Optional<Haiku> haiku,
                              Optional<Image> image) {

  public static HaikuGeneration empty(HaikuId haikuId) {
    return new HaikuGeneration(haikuId, "***********", null, STARTED, Optional.empty(), Optional.empty());
  }

  public HaikuGeneration withImageUrl(String imageUrl) {
    return new HaikuGeneration(haikuId, userInput, generatedAt, IMAGE_GENERATED, haiku, Optional.of(new Image(imageUrl)));
  }

  public HaikuGeneration withUserInput(String userInput) {
    return new HaikuGeneration(haikuId, userInput, generatedAt, status, haiku, image) ;
  }

  public HaikuGeneration withHaiku(Haiku haiku) {
    return new HaikuGeneration(haikuId, userInput, Instant.now().toEpochMilli(), HAIKU_GENERATED, Optional.of(haiku), image);
  }

  public boolean isComplete() {
    return image.isPresent();
  }

  public HaikuGeneration accepted() {
    return new HaikuGeneration(haikuId, userInput, generatedAt, ACCEPTED, haiku, image);
  }

  public HaikuGeneration toxicityDetected() {
    return new HaikuGeneration(haikuId, userInput, Instant.now().toEpochMilli(), TOXICITY_DETECTED, haiku, Optional.of(new Image("static/img/censored.png")));
  }

  public HaikuGeneration negativityDetected() {
    return new HaikuGeneration(haikuId, userInput, Instant.now().toEpochMilli(), NEGATIVITY_DETECTED, haiku, Optional.of(new Image("static/img/censored.png")));
  }

  public HaikuGeneration validated() {
    return new HaikuGeneration(haikuId, userInput, generatedAt, VALIDATED, haiku, image);
  }

  public HaikuGeneration timedOut() {
    return new HaikuGeneration(haikuId, userInput, Instant.now().toEpochMilli(), TIMED_OUT, haiku, Optional.of(new Image("static/img/time-is-up.png")));
  }

  public boolean successfullyGenerated() {
    return status != TOXICITY_DETECTED && status != NEGATIVITY_DETECTED && status != TIMED_OUT;
  }
}
