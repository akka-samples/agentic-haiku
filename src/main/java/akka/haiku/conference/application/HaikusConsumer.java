package akka.haiku.conference.application;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.haiku.generator.domain.HaikuGeneration;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.javasdk.http.HttpClient;
import akka.javasdk.http.HttpClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * This consumer listen to completed haikus workflows and
 * prepare the content to be published to Bluesky
 */
@ComponentId("haikus-consumer")
@Consume.FromWorkflow(HaikuGenerationWorkflow.class)
public class HaikusConsumer extends Consumer {


  private final HttpClient httpClient;
  private final ComponentClient componentClient;

  private final Logger logger = LoggerFactory.getLogger(HaikusConsumer.class);

  public HaikusConsumer(HttpClientProvider httpClientProvider, ComponentClient componentClient) {
    this.httpClient = httpClientProvider.httpClientFor("https://dvbe25.cfp.dev");
    this.componentClient = componentClient;
  }

  public Effect onChange(HaikuGeneration haikuGen) {

    if (haikuGen.isComplete()) {
      logger.debug("Preparing haiku social post for: {}", haikuGen.haikuId());
      try {

        Optional<Proposal> proposal = Optional.empty();

        if (haikuGen.haikuId().isTalk()) {
          var proposalId = haikuGen.haikuId().extractTalkId();
          proposal = Optional.of(fetchProposal(proposalId));
        }

        var blueskyUsers = proposal.map(p ->
          p.speakers().stream()
            .map(Speaker::blueskyUsername)
            .filter(handle -> handle != null && !handle.trim().isEmpty())
            .toList()
        ).orElseGet(List::of);

        var scheduleTime = calculateSchedule(proposal);

        var post = SocialPostEntity.SocialPostState.of(
          haikuGen.haiku().get().formatted(),
          haikuGen.image().get().url(),
          contextTags(proposal),
          blueskyUsers,
          scheduleTime);

          logger.info("Creating post for: {}.", haikuGen.haikuId());

          String postId = haikuGen.haikuId().isTalk() ?
            haikuGen.haikuId().extractTalkId() :
            haikuGen.haikuId().id();

          componentClient
            .forKeyValueEntity(postId)
            .method(SocialPostEntity::createPost)
            .invoke(post);

      } catch (Exception e) {
        logger.error("Error fetching haiku: {}", haikuGen.haikuId(), e);
      }

      return effects().done();
    }

    return effects().ignore();

  }

  private Instant calculateSchedule(Optional<Proposal> proposal) {

    // TODO: what if timeslot is empty?
    if (proposal.isPresent()) {
      var timeslot = proposal.get().timeSlots().getFirst();
      ZoneId belgiumZone = ZoneId.of(timeslot.timezone());
      ZonedDateTime belgiumTime = timeslot.fromDate().atZone(belgiumZone);
      return belgiumTime.toInstant().minus(Duration.ofMinutes(45));

    } else {
      return Instant.now().plus(Duration.ofSeconds(20));
    }
  }

  private List<String> contextTags(Optional<Proposal> proposal) {
    if (proposal.isPresent()) {
        if (proposal.get().timeSlots().isEmpty()) {
        var room = proposal.get().timeSlots().getFirst().roomName();
        return List.of("akka", room.replace(" ", "").toLowerCase());
      }
    }

    return List.of("akka");
  }

  private Proposal fetchProposal(String proposalId) {
    return
      httpClient.GET("/api/public/talks/" + proposalId).responseBodyAs(Proposal.class).invoke().body();
  }
}
