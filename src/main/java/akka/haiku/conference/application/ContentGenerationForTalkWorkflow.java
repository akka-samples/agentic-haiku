package akka.haiku.conference.application;

import akka.Done;
import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpClient;
import akka.javasdk.http.HttpClientProvider;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ComponentId("run-content-generation-for-talk")
public class ContentGenerationForTalkWorkflow extends Workflow<ContentGenerationForTalkWorkflow.ContentGenerationForTalk> {

  private final Logger log = LoggerFactory.getLogger(ContentGenerationForTalkWorkflow.class);
  private final String talkId;
  private final ComponentClient componentClient;
  private final HttpClient httpClient;

  public ContentGenerationForTalkWorkflow(WorkflowContext workflowContext,
                                          ComponentClient componentClient,
                                          HttpClientProvider httpClientProvider) {
    this.talkId = workflowContext.workflowId();
    this.componentClient = componentClient;
    this.httpClient = httpClientProvider.httpClientFor("https://dvbe25.cfp.dev");
  }


  record ContentGenerationForTalk(String talkId, boolean running) {

    public ContentGenerationForTalk asRunning() {
      return new ContentGenerationForTalk(talkId, true);
    }

    public ContentGenerationForTalk asIdle() {
      return new ContentGenerationForTalk(talkId, false);
    }
  }

  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepRecovery(maxRetries(3).failoverTo(ContentGenerationForTalkWorkflow::abort))
      .build();
  }

  @Override
  public ContentGenerationForTalk emptyState() {
    return new ContentGenerationForTalk(talkId, false);
  }

  public Effect<Done> start() {
    if (currentState().running)
      return effects().reply(Done.getInstance());
    else
      return effects()
        .updateState(currentState().asRunning())
        .transitionTo(ContentGenerationForTalkWorkflow::fetchTalk)
        .thenReply(Done.getInstance());
  }

  private StepEffect fetchTalk() {
    log.info("Fetching talk with id {}", talkId);
    var proposal =
      httpClient.GET("/api/public/talks/" + talkId).responseBodyAs(Proposal.class).invoke().body();

    return stepEffects()
      .thenTransitionTo(ContentGenerationForTalkWorkflow::selectBuzzWords)
      .withInput(proposal.description());
  }

  private StepEffect selectBuzzWords(String talkDescription) {
    var selection =
    this.componentClient
      .forAgent()
      .inSession(UUID.randomUUID().toString())
      .method(TalkBuzzSelectorAgent::extraBuzzWords)
      .invoke(talkDescription);

    return stepEffects()
      .thenTransitionTo(ContentGenerationForTalkWorkflow::generateHaiku)
      .withInput(selection);
  }


  private StepEffect generateHaiku(String words) {
    // each haiku generation ID is composed by talk id + a random
    // later, when the Haiku is ready, we will extract the ID and send the Haiku to the speakers
    this.componentClient
      .forWorkflow(talkId + ":" + UUID.randomUUID().toString())
      .method(HaikuGenerationWorkflow::start)
      .invoke(new HaikuGenerationWorkflow.StartGeneration(words));

    return stepEffects()
      .updateState(currentState().asIdle())
      .thenPause();
  }

  private StepEffect abort() {
    return stepEffects()
      .updateState(currentState().asIdle())
      .thenPause();
  }
}
