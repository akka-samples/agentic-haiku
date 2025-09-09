package com.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import com.example.domain.TextInputCollectorEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@ComponentId("inputs-view")
public class InputsViews extends View {

  public record CollectedInputs(Instant createdAt, String collectorId, String collectionId, List<String> inputs) {}

  public record CollectedInputsEntries(Collection<CollectedInputs> entries) {}

  @Consume.FromEventSourcedEntity(value = TextInputCollectorEntity.class, ignoreUnknown = true)
  public static class ImagesUpdater extends TableUpdater<CollectedInputs> {

    public Effect<CollectedInputs> onEvent(TextInputCollectorEvent.AllInputsCollected allInputsCollected) {
      var createdAt = updateContext().metadata().asCloudEvent().time().orElseThrow().toInstant();
      return effects().updateRow(new CollectedInputs(createdAt, allInputsCollected.collectorId(), allInputsCollected.collectionId(), allInputsCollected.inputs()));
    }
  }

  @Query(value = "SELECT * from inputs", streamUpdates = true)
  public QueryStreamEffect<CollectedInputs> getStream() {
    return queryStreamResult();
  }
}
