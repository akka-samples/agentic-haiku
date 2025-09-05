package com.example.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ContentGeneration(List<String> inputs,String prompt, Long generatedAt, Optional<Haiku> haiku, Optional<Image> image) {

  public static ContentGeneration of(List<String> inputs) {
    return new ContentGeneration(inputs, "", null,  Optional.empty(), Optional.empty());
  }

  public ContentGeneration withImagePrompt(String prompt) {
    return new ContentGeneration(inputs, prompt, generatedAt, haiku, image);
  }

  public ContentGeneration withImageUrl(String imageUrl) {
    return new ContentGeneration(inputs, prompt, Instant.now().toEpochMilli(), haiku, Optional.of(new Image(imageUrl)));
  }
}
