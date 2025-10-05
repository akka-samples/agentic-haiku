package akka.haiku.generator.api;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.haiku.generator.application.HaikuView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.http.Delete;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;

import java.util.List;

@HttpEndpoint("/haikus-backoffice")
public record HaikuBackofficeEndpoint(ComponentClient componentClient) {

  @Get
  public List<HaikuView.GeneratedHaiku> get() {
    return componentClient.forView()
      .method(HaikuView::getAll)
      .invoke()
      .items();
  }

  @Delete("/{haikuId}")
  public HttpResponse delete(String haikuId) {
    componentClient.forWorkflow(haikuId)
      .method(HaikuGenerationWorkflow::delete)
      .invoke();
    return HttpResponses.ok();
  }
}
