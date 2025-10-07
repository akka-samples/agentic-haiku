package akka.haiku.conference.application;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.haiku.generator.domain.HaikuGeneration;
import akka.haiku.generator.domain.HaikuId;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
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

        // collect full names from speakers without bsky
        var names = proposal.map(p ->
          p.speakers().stream()
            .filter(speaker -> speaker.blueskyUsername() == null || speaker.blueskyUsername().trim().isEmpty())
            .map(Speaker::fullName)
            .toList()
        ).orElseGet(List::of);


        if (haikuGen.haiku().isPresent() && haikuGen.image().isPresent()) {

          var scheduleTime =
            calculateSchedule(proposal)
              .orElse(Instant.now().plus(Duration.ofMinutes(10)));

          var post = SocialPostEntity.SocialPostState.of(
            haikuGen.haiku().get().formatted(),
            haikuGen.image().get().url(),
            contextTags(proposal),
            names,
            blueskyUsers,
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

  private Optional<Instant> calculateSchedule(Optional<Proposal> proposalOpt) {
    return proposalOpt.flatMap( proposal -> {
      if (proposal.timeSlots().isEmpty()) { return Optional.empty(); }
      else {
      var timeslot = proposal.timeSlots().getFirst();
      ZoneId belgiumZone = ZoneId.of(timeslot.timezone());
      ZonedDateTime belgiumTime = timeslot.fromDate().atZone(belgiumZone);

      // randomly throttling schedule to avoid being detected as spammer by bsky
      int randomMinutes = 20 + (int) (Math.random() * 31); // 20 to 50 inclusive
      return Optional.of(belgiumTime.toInstant().minus(Duration.ofMinutes(randomMinutes)));
      }
    });
  }

  private List<String> contextTags(Optional<Proposal> proposal) {
    if (proposal.isPresent()) {
        if (!proposal.get().timeSlots().isEmpty()) {
        var room = proposal.get().timeSlots().getFirst().roomName();
        return List.of("Devoxx", "Akka", room.replace(" ", "").toLowerCase());
      }
    }

    return List.of("Devoxx", "Akka");
  }

  private Proposal fetchProposal(String proposalId) {
    return
      httpClient.GET("/api/public/talks/" + proposalId).responseBodyAs(Proposal.class).invoke().body();
  }
}
