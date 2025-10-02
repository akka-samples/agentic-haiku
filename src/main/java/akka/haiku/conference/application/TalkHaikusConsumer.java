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

import java.util.List;
import java.util.Optional;

@ComponentId("haiku-social-publisher")
@Consume.FromWorkflow(HaikuGenerationWorkflow.class)
public class TalkHaikusConsumer extends Consumer {


  private final HttpClient httpClient;
  private final ComponentClient componentClient;

  private final Logger logger = LoggerFactory.getLogger(TalkHaikusConsumer.class);

  public TalkHaikusConsumer(HttpClientProvider httpClientProvider, ComponentClient componentClient) {
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

        var xHandlers = proposal.map(p ->
          p.speakers().stream()
            .map(Speaker::twitterHandle)
            .filter(handle -> handle != null && !handle.trim().isEmpty())
            .toList()).orElseGet(List::of);


        var blueskyUsers = proposal.map(p ->
          p.speakers().stream()
            .map(Speaker::blueskyUsername)
            .filter(handle -> handle != null && !handle.trim().isEmpty())
            .toList()
        ).orElseGet(List::of);

        var post = SocialPostEntity.SocialPostState.of(
          haikuGen.haiku().get().formatted(),
          haikuGen.image().get().url(),
          List.of("#akka", "#devoxx"),
          xHandlers, blueskyUsers);

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

  private Proposal fetchProposal(String proposalId) {
    return
      httpClient.GET("/api/public/talks/" + proposalId).responseBodyAs(Proposal.class).invoke().body();
  }
}
