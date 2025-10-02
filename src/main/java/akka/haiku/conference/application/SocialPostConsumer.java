package akka.haiku.conference.application;


import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.javasdk.timer.TimerScheduler;

import java.time.Duration;


/**
 * Consumes Social post to prepare for scheduling.
 * They are scheduled as soon as they are created, but will only be posted if they are approved when
 * the schedule action fires.
 */
@ComponentId("social-post-consumer")
@Consume.FromKeyValueEntity(SocialPostEntity.class)
public class SocialPostConsumer extends Consumer {

  private final ComponentClient componentClient;
  private final TimerScheduler  scheduler;

  public SocialPostConsumer(ComponentClient componentClient, TimerScheduler scheduler) {
    this.componentClient = componentClient;
    this.scheduler = scheduler;
  }

  public Effect onChange(SocialPostEntity.SocialPostState postState) {

    var postId = messageContext().eventSubject().get();
    // as soon as the post is created, we schedule it for publishing

    if (postState.created()) {
        scheduler.createSingleTimer(
          postId,
          postState.scheduleTime(),
          componentClient.forTimedAction()
            .method(SocialPublisherAction::publishSocialPost)
            .deferred(postId)
        );
    }

    return effects().done();
  }
}
