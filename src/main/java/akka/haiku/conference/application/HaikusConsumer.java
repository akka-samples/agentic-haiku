package akka.haiku.conference.application;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.haiku.generator.domain.HaikuGeneration;
import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * This consumer listen to completed haikus workflows and
 * prepare the content to be published to Bluesky
 */
@Component(id = "haikus-consumer")
@Consume.FromWorkflow(HaikuGenerationWorkflow.class)
public class HaikusConsumer extends Consumer {


  private final ComponentClient componentClient;

  private final Logger logger = LoggerFactory.getLogger(HaikusConsumer.class);

  public HaikusConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onChange(HaikuGeneration haikuGen) {

    if (haikuGen.isComplete()) {
      logger.debug("Preparing haiku social post for: {}", haikuGen.haikuId());
      try {
        if (haikuGen.haiku().isPresent() && haikuGen.image().isPresent()) {

          var now = Instant.now();
          var scheduleTime = now.plus(Duration.ofMinutes(10));

          var post = SocialPostEntity.SocialPostState.of(
            haikuGen.haiku().get().formatted(),
            haikuGen.image().get().url(), contextTags(), List.of(), List.of(),
            scheduleTime);

          logger.info("Creating post for: {}.", haikuGen.haikuId());

          componentClient
            .forKeyValueEntity(haikuGen.haikuId().id())
            .method(SocialPostEntity::createPost)
            .invoke(post);
        }

      } catch (Exception e) {
        logger.error("Error fetching haiku: {}", haikuGen.haikuId(), e);
      }

      return effects().done();
    }

    return effects().ignore();

  }

  @DeleteHandler
  public Effect onDelete() {
    var id = messageContext().eventSubject().get();
    componentClient
      .forKeyValueEntity(id)
      .method(SocialPostEntity::delete)
      .invoke();

    return effects().done();
  }

  private List<String> contextTags() {
    return List.of("Akka");
  }
}
