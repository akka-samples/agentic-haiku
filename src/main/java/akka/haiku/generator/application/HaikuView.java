package akka.haiku.generator.application;

import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.HaikuGeneration;
import akka.haiku.generator.domain.Image;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

import java.util.List;
import java.util.Optional;

@ComponentId("generated-image-view")
public class HaikuView extends View {


  public record GeneratedHaiku(String id,
                               String prompt,
                               Long generatedAt,
                               Optional<Haiku> haiku,
                               Optional<Image> image,
                               boolean deleted){

    public GeneratedHaiku asDeleted() {
      return new GeneratedHaiku(id, prompt, generatedAt, haiku, image, true);
    }
  }

  public record GeneratedContentItems(List<GeneratedHaiku> items){}

  @Consume.FromWorkflow(HaikuGenerationWorkflow.class)
  public static class ImagesUpdater extends TableUpdater<GeneratedHaiku> {

    public Effect<GeneratedHaiku> onChange(HaikuGeneration haikuGeneration) {
      if (haikuGeneration.image().isPresent() && haikuGeneration.successfullyGenerated()) {

        var id = updateContext().eventSubject().get();
        var content =
          new GeneratedHaiku(id,
            haikuGeneration.userInput(),
            haikuGeneration.generatedAt(),
            haikuGeneration.haiku(),
            haikuGeneration.image(),
            false);

        return effects().updateRow(content);
      } else {
        return effects().ignore();
      }
    }

    @DeleteHandler
    public Effect<GeneratedHaiku> onDelete() {
      return effects().updateRow(rowState().asDeleted());
    }
  }

  @Query(value = "SELECT * FROM images WHERE deleted = false", streamUpdates = true)
  public QueryStreamEffect<GeneratedHaiku> get() {
    return queryStreamResult();
  }

  @Query(value = "SELECT * as items FROM images ORDER BY generatedAt DESC")
  public QueryEffect<GeneratedContentItems> getAll() {
    return queryResult();
  }
}
