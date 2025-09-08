package com.example.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.example.domain.TextInputCollector;
import com.example.domain.TextInputCollectorEvent;
import com.example.domain.TextInputCollectorEvent.AllInputsCollected;
import com.example.domain.TextInputCollectorEvent.TextInputAdded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static akka.Done.done;

@ComponentId("text-input-collector")
public class TextInputCollectorEntity
  extends EventSourcedEntity<TextInputCollector, TextInputCollectorEvent> {

  private static final Logger log = LoggerFactory.getLogger(TextInputCollectorEntity.class);
  private final EventSourcedEntityContext context;
  private final int limit = 2;
  private final String collectorId;

  public TextInputCollectorEntity(EventSourcedEntityContext context) {
    this.collectorId = context.entityId();
    this.context = context;
  }

  @Override
  public TextInputCollector emptyState() {
    return new TextInputCollector(collectorId, limit, new ArrayList<>());
  }

  // Command handlers
  public Effect<Done> addTextInput(String input) {
    log.info("{}, adding text input [{}]", collectorId, input);
    if (currentState().inputs().size() + 1 == limit) {
      List<String> inputs = new ArrayList<>();
      inputs.addAll(currentState().inputs());
      inputs.add(input);
      return effects()
        .persist(new TextInputAdded(input),
          new AllInputsCollected(currentState().collectorId(), UUID.randomUUID().toString(), inputs))
        .thenReply(__ -> done());
    } else {
      return effects()
        .persist(new TextInputAdded(input))
        .thenReply(__ -> done());
    }

  }

  public Effect<TextInputCollector> getState() {
    return effects().reply(currentState());
  }

  @Override
  public TextInputCollector applyEvent(TextInputCollectorEvent event) {
    return currentState().apply(event);
  }
}