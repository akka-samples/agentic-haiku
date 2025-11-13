package akka.haiku.conference.application;

import akka.javasdk.annotations.Component;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timedaction.TimedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(id = "post-publisher")
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
    try {
      var post =
        componentClient
          .forKeyValueEntity(postId)
          .method(SocialPostEntity::getPost).invoke();

      if (post.notRejected()) {
        log.debug("Publishing on BlueSky platform {}, handlers [{}]", postId, post.bskyHandlers());
        socialPublisher.publish(post.post(), post.imageUrl(), post.tags(), post.names(), post.bskyHandlers());

        // if we publish it, we mark it as approve so it is removed from the queue list
        componentClient
          .forKeyValueEntity(postId)
          .method(SocialPostEntity::approvePost).invoke();
      }

    } catch (PostNotFoundException e) {
      return effects().done();
    }
    return effects().done();
  }
}
