package akka.haiku.conference.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timedaction.TimedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ComponentId("post-publisher")
public class SocialPublisherAction extends TimedAction {

  private final Logger log = LoggerFactory.getLogger(SocialPublisherAction.class);
  private final ComponentClient componentClient;
  private final SocialPublisher socialPublisher;

  public SocialPublisherAction(ComponentClient componentClient,
                               SocialPublisher socialPublisher) {
    this.componentClient = componentClient;
    this.socialPublisher = socialPublisher;

  }

  public Effect publishSocialPost(String postId) {
    var post =
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::getPost).invoke();

    if (post.notRejected()) {
      log.debug("Publishing on BlueSky platform {}, handlers [{}]", postId, post.bskyHandlers());
      socialPublisher.publish(post.post(), post.imageUrl(), List.of(), List.of());
    } else {
      log.debug("Post not approved on time. Skipping publishing to BlueSky platform {}, handlers [{}]", postId,
        post.bskyHandlers());
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::rejectPost).invoke();
    }
    return effects().done();
  }
}
