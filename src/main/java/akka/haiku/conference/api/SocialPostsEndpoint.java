package akka.haiku.conference.api;

import akka.Done;
import akka.haiku.conference.application.SocialPostEntity;
import akka.haiku.conference.application.SocialPostView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HttpEndpoint("/posts")
public class SocialPostsEndpoint {


  private final ComponentClient componentClient;
  private Logger log =  LoggerFactory.getLogger(SocialPostsEndpoint.class);


  public SocialPostsEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }


  @Get
  public HttpResponse posts() {
    var contentUpdates = componentClient.forView()
      .stream(SocialPostView::getOpenPosts)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }

  @Post("/{postId}/reject")
  public Done rejet(String postId) {
    log.info("Rejecting Haiku for {}", postId);
    return componentClient
      .forKeyValueEntity(postId).method(SocialPostEntity::rejectPost)
      .invoke();
  }

  @Post("/{postId}/publish")
  public Done publish(String postId) {
    log.info("Publishing Haiku for {}", postId);
    return componentClient
      .forKeyValueEntity(postId).method(SocialPostEntity::publish)
      .invoke();
  }
}
