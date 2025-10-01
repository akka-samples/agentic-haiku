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

    if (haikuGen.isComplete() && haikuGen.haikuId().isTalk()) {
      logger.debug("Preparing haiku social post for: {}", haikuGen.haikuId());
      try {
        var proposalId = haikuGen.haikuId().extractTalkId();
        var proposal = fetchProposal(proposalId);

        var xHandlers =
          proposal.speakers().stream()
            .map(Speaker::twitterHandle)
            .filter(handle -> handle != null && !handle.trim().isEmpty())
            .toList();

        var blueskyUsers =
          proposal.speakers().stream()
            .map(Speaker::blueskyUsername)
            .filter(handle -> handle != null && !handle.trim().isEmpty())
            .toList();

        var post = SocialPostEntity.SocialPostState.of(
          haikuGen.haikuId().extractTalkId(),
          haikuGen.haiku().get().formatted(),
          haikuGen.image().get().url(),
          List.of("#akka", "#devoxx")
        );


        if (xHandlers.isEmpty() && blueskyUsers.isEmpty()) {
          logger.info("Creating post for: {}. No social handlers found", haikuGen.haikuId());
          componentClient
            .forKeyValueEntity(proposalId)
            .method(SocialPostEntity::createPost)
            .invoke(post);
        } else {

          if (!xHandlers.isEmpty()) {
            logger.info("Creating post for: {}. For X: [{}]", haikuGen.haikuId(), xHandlers);
            componentClient
              .forKeyValueEntity("x:" + proposalId)
              .method(SocialPostEntity::createPost)
              .invoke(post.withUsers(xHandlers));
          }

          if (!blueskyUsers.isEmpty()) {
            logger.info("Creating post for: {}. For BSky: [{}]", haikuGen.haikuId(), xHandlers);
            componentClient
              .forKeyValueEntity("bsky:" + proposalId)
              .method(SocialPostEntity::createPost)
              .invoke(post.withUsers(blueskyUsers));
          }
        }

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
