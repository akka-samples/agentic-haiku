package com.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import com.example.domain.ContentGeneration;
import com.example.domain.Haiku;
import com.example.domain.Image;

import java.util.Optional;

@ComponentId("generated-image-view")
public class GeneratedContentView extends View {


  public record GeneratedContent(Long generateAt, String prompt, Optional<Haiku> haiku, Optional<Image> image){}

  @Consume.FromWorkflow(ContentGenerationWorkflow.class)
  public static class ImagesUpdater extends TableUpdater<GeneratedContent> {

    public Effect<GeneratedContent> onChange(ContentGeneration contentGeneration) {
      if (contentGeneration.haiku().isPresent() || contentGeneration.image().isPresent()) {
        var content = new GeneratedContent(contentGeneration.generatedAt(), contentGeneration.prompt(), contentGeneration.haiku(), contentGeneration.image());
        return effects().updateRow(content);
      } else {
        return effects().ignore();
      }
    }
  }

  @Query(value = "SELECT * from images", streamUpdates = true)
  public QueryStreamEffect<GeneratedContent> get() {
    return queryStreamResult();
  }
}
