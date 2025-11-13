package akka.haiku.conference.application;


import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.javasdk.timer.TimerScheduler;

import java.time.Duration;
import java.time.Instant;


/**
 * Consumes Social post to prepare for scheduling.
 * They are scheduled as soon as they are created, but will only be posted if they are approved when
 * the schedule action fires.
 */
@Component(id = "social-post-consumer")
@Consume.FromKeyValueEntity(SocialPostEntity.class)
public class SocialPostConsumer extends Consumer {

  private final ComponentClient componentClient;
  private final TimerScheduler  scheduler;

  public SocialPostConsumer(ComponentClient componentClient, TimerScheduler scheduler) {
    this.componentClient = componentClient;
    this.scheduler = scheduler;
  }

  // as soon as the post is created, we schedule it for publishing
  public Effect onChange(SocialPostEntity.SocialPostState postState) {

    var postId = messageContext().eventSubject().get();
    var scheduleTime = Duration.between(Instant.now(), postState.scheduleTime());

    if (postState.created()) {
        scheduler.createSingleTimer(
          postId,
          scheduleTime,
          componentClient.forTimedAction()
            .method(SocialPublisherAction::publishSocialPost)
            .deferred(postId)
        );
    }

    return effects().done();
  }

  @DeleteHandler
  public Effect onDelete() {
    var postId = messageContext().eventSubject().get();
    scheduler.delete(postId);
    return effects().done();
  }
}
