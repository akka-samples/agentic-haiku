package akka.haiku.generator.api;

import akka.haiku.generator.application.AgentTeamWorkflow;
import akka.haiku.generator.application.HaikuView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Put;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered,
// and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/haikus")
public record AkkaHaikusEndpoint(ComponentClient componentClient) {

  public record Input(String message) {}

  @Put("/{haikuId}")
  public HttpResponse create(String haikuId, Input input) {

    var response =
      componentClient.forWorkflow(haikuId)
        .method(AgentTeamWorkflow::start)
        .invoke(new AgentTeamWorkflow.StartGeneration(input.message));

    return HttpResponses.ok();
  }

  /**
   * Return
   */
  @Get
  public HttpResponse realTimeContent() {
    var contentUpdates = componentClient.forView()
      .stream(HaikuView::get)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }
}
