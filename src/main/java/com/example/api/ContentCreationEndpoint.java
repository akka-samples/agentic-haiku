package com.example.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.example.application.GeneratedContentView;
import com.example.application.InputsViews;
import com.example.application.TextInputCollectorEntity;

import java.util.List;

/**
 * This is a simple Akka Endpoint that uses an agent and LLM to generate
 * greetings in different languages.
 */
// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered,
// and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint
public class ContentCreationEndpoint {

  public record AddInputRequest(String input) {}

  private final ComponentClient componentClient;

  public ContentCreationEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/inputs")
  public HttpResponse addInput(AddInputRequest request) {

    //TODO add validation here

    componentClient
      .forEventSourcedEntity("collectorA")
      .method(TextInputCollectorEntity::addTextInput)
      .invoke(request.input);

    return HttpResponses.ok();
  }

  @Get("/inputs")
  public List<String> get() {

    var collectorState = componentClient
      .forEventSourcedEntity("collectorA")
      .method(TextInputCollectorEntity::getState)
      .invoke();

    return collectorState.inputs();
  }

  @Get("/collected-inputs-stream")
  public HttpResponse getCollected() {

    var collectedInputUpdates = componentClient
      .forView()
      .stream(InputsViews::getStream)
      .source();

    return HttpResponses.serverSentEvents(collectedInputUpdates);
  }

  @Get("/content-stream")
  public HttpResponse realTimeContent() {

    var contentUpdates = componentClient.forView()
      .stream(GeneratedContentView::get)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }
}
