package akka.haiku.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import akka.haiku.domain.TextInputCollector;
import akka.haiku.domain.TextInputCollectorEvent;
import akka.haiku.domain.TextInputCollectorEvent.TextInputAdded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import static akka.Done.done;

@ComponentId("text-prompt-collector")
public class TextInputCollectorEntity
  extends EventSourcedEntity<TextInputCollector, TextInputCollectorEvent> {

  private static final Logger log = LoggerFactory.getLogger(TextInputCollectorEntity.class);
  private final EventSourcedEntityContext context;
  private final int limit = 10;
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
    log.info("{}, adding text prompt [{}]", collectorId, input);
    return effects()
      .persist(new TextInputAdded(collectorId, UUID.randomUUID().toString(), input))
      .thenReply(__ -> done());

  }

  public Effect<TextInputCollector> getState() {
    return effects().reply(currentState());
  }

  @Override
  public TextInputCollector applyEvent(TextInputCollectorEvent event) {
    return currentState().apply(event);
  }
}