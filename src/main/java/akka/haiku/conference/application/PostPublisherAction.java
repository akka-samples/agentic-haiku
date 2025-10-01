package akka.haiku.conference.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timedaction.TimedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("post-publisher")
public class PostPublisherAction extends TimedAction {

  private final Logger log = LoggerFactory.getLogger(PostPublisherAction.class);
  private final ComponentClient componentClient;

  public PostPublisherAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect publishOnX(String postId) {
    var post =
    componentClient
      .forKeyValueEntity(postId)
      .method(SocialPostEntity::getPost).invoke();

    if (post.approved()) {
      log.debug("Publishing on X platform {}, handlers [{}]", postId, post.xHandlers());
    } else {
      log.debug("Not publishing to X platform {}, handlers [{}]", postId, post.bskyHandlers());
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::rejectPost).invoke();
    }
    return effects().done();
  }

  public Effect publishOnBsky(String postId) {
    var post =
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::getPost).invoke();

    if (post.approved()) {
      log.debug("Publishing on BlueSky platform {}, handlers [{}]", postId, post.bskyHandlers());
    } else {
      log.debug("Not publishing to BlueSky platform {}, handlers [{}]", postId, post.bskyHandlers());
      componentClient
        .forKeyValueEntity(postId)
        .method(SocialPostEntity::rejectPost).invoke();
    }
    return effects().done();
  }
}
