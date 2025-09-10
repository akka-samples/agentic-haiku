package akka.haiku.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import akka.haiku.domain.TextInputCollectorEvent;

import java.time.Instant;

@ComponentId("inputs-view")
public class InputsViews extends View {

  public record CollectedInputs(Instant createdAt, String eventId, String inputId, String input) {}

  @Consume.FromEventSourcedEntity(value = TextInputCollectorEntity.class, ignoreUnknown = true)
  public static class ImagesUpdater extends TableUpdater<CollectedInputs> {

    public Effect<CollectedInputs> onEvent(TextInputCollectorEvent.TextInputAdded textInputAdded) {
      var createdAt = updateContext().metadata().asCloudEvent().time().orElseThrow().toInstant();
      return effects().updateRow(new CollectedInputs(createdAt, textInputAdded.collectorId(), textInputAdded.inputId(), textInputAdded.input()));
    }
  }

  @Query(value = "SELECT * from inputs", streamUpdates = true)
  public QueryStreamEffect<CollectedInputs> getStream() {
    return queryStreamResult();
  }
}
