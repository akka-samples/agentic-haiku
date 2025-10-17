package akka.haiku.generator.api;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered,
// and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/haikus")
public record HaikusEndpoint(ComponentClient componentClient, Materializer materializer) {

  private static final Logger log = LoggerFactory.getLogger(HaikusEndpoint.class);

  public record Input(String text) {
  }

  @Post("/{haikuId}")
  public HttpResponse create(String haikuId, Input input) {

    componentClient.forWorkflow(haikuId)
      .method(HaikuGenerationWorkflow::start)
      .invoke(input.text);

    return HttpResponses.ok();
  }

  @Get("/{haikuId}")
  public HaikuResponse getHaiku(String haikuId) {

    var state =
      componentClient.forWorkflow(haikuId)
        .method(HaikuGenerationWorkflow::getState)
        .invoke();

    return new HaikuResponse(
      haikuId,
      state.userInput(),
      state.generatedAt(),
      state.haiku(),
      state.image());
  }
}
