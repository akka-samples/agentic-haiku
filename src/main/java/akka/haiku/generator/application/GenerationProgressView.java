package akka.haiku.generator.application;

import akka.haiku.generator.domain.ContentGeneration;
import akka.haiku.generator.domain.ContentGenerationStatus;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

import java.util.List;
import java.util.stream.Stream;

@ComponentId("generation-progress-view")
public class GenerationProgressView extends View {

  public record Progress(String haikuId, boolean completed, List<String> lines) {
    public Progress addLines(List<String> progressLines) {
      return new Progress(haikuId, completed, Stream.concat(lines.stream(), progressLines.stream()).toList());
    }

    public Progress setCompleted(boolean complete) {
      return new Progress(haikuId, complete, lines);
    }
  }

  @Query("SELECT * from generation_progress WHERE haikuId = :haikuId")
  public QueryEffect<Progress> get(String haikuId) {
    return queryResult();
  }

  @Consume.FromWorkflow(AgentTeamWorkflow.class)
  public static class GenerationProgressUpdater extends TableUpdater<Progress> {

    public Effect<Progress> onChange(ContentGeneration contentGeneration) {
      if (rowState() == null) {
        if (contentGeneration.status() == null) { //backward compatibility, remove when deploying final version
          return effects().ignore();
        }
        var id = updateContext().eventSubject().get();
        return effects().updateRow(new Progress(id, false, toProgressLines(contentGeneration.status())));
      } else {
        if (contentGeneration.status() == null) { //backward compatibility, remove when deploying final version
          return effects().ignore();
        }
        return effects().updateRow(rowState()
          .addLines(toProgressLines(contentGeneration.status()))
          .setCompleted(contentGeneration.isComplete()));
      }
    }
  }

  private static List<String> toProgressLines(ContentGenerationStatus status) {
    return switch (status) {
      case STARTED -> List.of("Verifying message quality.");
      case ACCEPTED -> List.of("Message is accepted.", "Analysing message sentiment.");
      case TOXICITY_DETECTED ->
        List.of("Message is rejected. Please be more creative and remember to be polite and respectful.");
      case NEGATIVITY_DETECTED ->
        List.of("Message is rejected, because it is too negative. Please be more positive and remember to be polite and respectful.");
      case VALIDATED -> List.of("Message successfully validated, generating a Haiku.");
      case HAIKU_GENERATED -> List.of("Haiku generated.", "Generating Haiku image.");
      case IMAGE_GENERATED -> List.of("Haiku image generated.", "Come and collect your Akka t-shirt!");
      case TIMED_OUT -> List.of("Cancelling generation due to timeout.");
    };
  }

}
