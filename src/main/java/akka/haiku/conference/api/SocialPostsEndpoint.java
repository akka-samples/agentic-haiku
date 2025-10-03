package akka.haiku.conference.api;

import akka.Done;
import akka.haiku.conference.application.SocialPostEntity;
import akka.haiku.conference.application.SocialPostView;
import akka.haiku.conference.application.SocialPublisher;
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
  private final SocialPublisher socialPublisher;
  private Logger log =  LoggerFactory.getLogger(SocialPostsEndpoint.class);



  public SocialPostsEndpoint(ComponentClient componentClient, SocialPublisher socialPublisher) {
    this.componentClient = componentClient;
    this.socialPublisher = socialPublisher;
  }


  @Get
  public java.util.List<SocialPostView.SocialPostRow> posts() {
    return componentClient.forView()
      .method(SocialPostView::getAllPosts)
      .invoke()
      .items();
  }

  @Post("/{postId}/reject")
  public Done rejet(String postId) {
    log.info("Rejecting Haiku for {}", postId);
    return componentClient
      .forKeyValueEntity(postId).method(SocialPostEntity::rejectPost)
      .invoke();
  }

  @Post("/{postId}/approve")
  public Done approve(String postId) {
    log.info("Publishing Haiku for {}", postId);
    return componentClient
      .forKeyValueEntity(postId).method(SocialPostEntity::approvePost)
      .invoke();
  }

  /**
   * Backdoor to force premature publishing. Useful for testing.
   * Only available in dev-mode or through 'akka service proxy'
   */
  @Post("/{postId}/publish")
  public void publish(String postId) {

    var post =
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::getPost).invoke();

    socialPublisher.publish(post.post(), post.imageUrl(), post.tags(), post.names(), post.bskyHandlers());
  }
}
