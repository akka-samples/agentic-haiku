package com.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.application.ContentGenerationWorkflow.StartGeneration;
import com.example.domain.TextInputCollectorEvent;

@ComponentId("input-consumer")
@Consume.FromEventSourcedEntity(value = TextInputCollectorEntity.class, ignoreUnknown = true)
public class InputConsumer extends Consumer {

  private final ComponentClient componentClient;

  public InputConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(TextInputCollectorEvent.AllInputsCollected event) {

    String workflowId = event.collectorId() + messageContext().metadata().asCloudEvent().sequence().orElse(0L);

    componentClient.forWorkflow(workflowId)
      .method(ContentGenerationWorkflow::start)
      .invoke(new StartGeneration(event.inputs()));

    return effects().done();
  }
}
