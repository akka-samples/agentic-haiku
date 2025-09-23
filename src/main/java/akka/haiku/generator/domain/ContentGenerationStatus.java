package akka.haiku.generator.domain;

public enum ContentGenerationStatus {
  STARTED, ACCEPTED, TOXICITY_DETECTED, NEGATIVITY_DETECTED, VALIDATED, HAIKU_GENERATED, IMAGE_GENERATED, TIMED_OUT
}
