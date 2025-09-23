package akka.haiku.generator.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import akka.haiku.generator.domain.ContentGeneration;
import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.Image;

import java.util.Optional;

@ComponentId("generated-image-view")
public class HaikuView extends View {


  public record GeneratedContent(String id,
                                 String prompt,
                                 Long generatedAt,
                                 Optional<Haiku> haiku,
                                 Optional<Image> image){}

  @Consume.FromWorkflow(AgentTeamWorkflow.class)
  public static class ImagesUpdater extends TableUpdater<GeneratedContent> {

    public Effect<GeneratedContent> onChange(ContentGeneration contentGeneration) {
      if (contentGeneration.image().isPresent() && contentGeneration.successfullyGenerated()) {

        var id = updateContext().eventSubject().get();
        var content =
          new GeneratedContent(id,
            contentGeneration.userInput(),
            contentGeneration.generatedAt(),
            contentGeneration.haiku(),
            contentGeneration.image());

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
