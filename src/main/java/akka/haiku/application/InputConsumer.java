package akka.haiku.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.haiku.application.ContentGenerationWorkflow.StartGeneration;
import akka.haiku.domain.TextInputCollectorEvent;

@ComponentId("input-consumer")
@Consume.FromEventSourcedEntity(value = TextInputCollectorEntity.class, ignoreUnknown = true)
public class InputConsumer extends Consumer {

  private final ComponentClient componentClient;

  public InputConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(TextInputCollectorEvent.TextInputAdded event) {

    String workflowId = "workflow-" + messageContext().metadata().asCloudEvent().sequence().orElse(0L);

    componentClient.forWorkflow(workflowId)
      .method(ContentGenerationWorkflow::start)
      .invoke(new StartGeneration(event.input()));

    return effects().done();
  }
}
