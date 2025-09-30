package akka.haiku.conference.api;

import akka.haiku.conference.application.SocialPostView;
import akka.haiku.gateway.application.QrCodeView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;

@HttpEndpoint
public class SocialPostsEndpoint {


  private final ComponentClient componentClient;


  public SocialPostsEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }


  @Get("/posts")
  public HttpResponse posts() {
    var contentUpdates = componentClient.forView()
      .stream(SocialPostView::getOpenPosts)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }
}
