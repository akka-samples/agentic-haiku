package akka.haiku.generator.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import akka.haiku.generator.domain.ContentGeneration;
import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.Image;

import java.util.List;
import java.util.Optional;

@ComponentId("generated-image-view-2")
public class HaikuView extends View {


  public record GeneratedContent(String id,
                                 String prompt,
                                 Long generatedAt,
                                 Optional<Haiku> haiku,
                                 Optional<Image> image,
                                 boolean deleted){

    public GeneratedContent asDeleted() {
      return new GeneratedContent(id, prompt, generatedAt, haiku, image, true);
    }
  }

  public record GeneratedContentItems(List<GeneratedContent> items){}

  @Consume.FromWorkflow(HaikuGenerationWorkflow.class)
  public static class ImagesUpdater extends TableUpdater<GeneratedContent> {

    public Effect<GeneratedContent> onChange(ContentGeneration contentGeneration) {
      if (contentGeneration.image().isPresent() && contentGeneration.successfullyGenerated()) {

        var id = updateContext().eventSubject().get();
        var content =
          new GeneratedContent(id,
            contentGeneration.userInput(),
            contentGeneration.generatedAt(),
            contentGeneration.haiku(),
            contentGeneration.image(),
            false);

        return effects().updateRow(content);
      } else {
        return effects().ignore();
      }
    }

    @DeleteHandler
    public Effect<GeneratedContent> onDelete() {
      return effects().updateRow(rowState().asDeleted());
    }
  }

  @Query(value = "SELECT * FROM images WHERE deleted = false", streamUpdates = true)
  public QueryStreamEffect<GeneratedContent> get() {
    return queryStreamResult();
  }

  @Query(value = "SELECT * as items FROM images ORDER BY generatedAt DESC")
  public QueryEffect<GeneratedContentItems> getAll() {
    return queryResult();
  }
}
