package akka.haiku.generator.application;

import akka.Done;
import akka.haiku.generator.domain.ContentGeneration;
import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.UserInput;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static akka.Done.done;

@ComponentId("image-generation-workflow")
public class AgentTeamWorkflow extends Workflow<ContentGeneration> {

  private static final Logger log = LoggerFactory.getLogger(AgentTeamWorkflow.class);
  private final ComponentClient componentClient;
  private final String workflowId;
  private final ImageGenerator imageGenerator;

  public AgentTeamWorkflow(WorkflowContext workflowContext, ComponentClient componentClient, ImageGenerator imageGenerator) {
    this.componentClient = componentClient;
    this.workflowId =workflowContext.workflowId();
    this.imageGenerator = imageGenerator;
  }

  public record StartGeneration(String input) {
  }

  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepTimeout(Duration.ofSeconds(15))
      .build();
  }

  public Effect<Done> start(StartGeneration startGeneration) {
    if (currentState() != null) {
      log.info("Already in progress, ignoring");
      return effects().reply(done());
    } else {
      log.info("Starting image generation for input: {}", startGeneration);

      return effects()
        .updateState(ContentGeneration.empty())
        .transitionTo(AgentTeamWorkflow::checkForToxicContent)
        .withInput(UserInput.of(startGeneration.input))
        .thenReply(done());
    }
  }

  private StepEffect checkForToxicContent(UserInput userInput) {

    var evaluated =
      componentClient
        .forAgent()
        .inSession(this.workflowId)
        .method(ToxicityDetectorAgent::evaluateContent)
        .invoke(userInput);

    if (evaluated.isAccepted()) {
      log.debug("Content is accepted.");
      return stepEffects()
        .thenTransitionTo(AgentTeamWorkflow::analyseSentiment)
        .withInput(evaluated);

    } else {
      log.debug("Content is rejected.");
      return stepEffects()
        .thenTransitionTo(AgentTeamWorkflow::generateCensoredImage);
    }
  }

  private StepEffect analyseSentiment(UserInput userInput) {
    var evaluated =
      componentClient
        .forAgent()
        .inSession(this.workflowId)
        .method(SentimentDetectorAgent::analyseSentiment)
        .invoke(userInput);

    if (evaluated.isNegative()) {
      log.debug("Content is negative, discarding it.");
      return stepEffects()
        .thenTransitionTo(AgentTeamWorkflow::generateCensoredImage);

    } else {
      log.debug("Content is positive or neutral, generating a Haiku...");
      return stepEffects()
        .updateState(currentState().withUserInput(userInput.originalInput()))
        .thenTransitionTo(AgentTeamWorkflow::generateHaiku)
        .withInput(evaluated);
    }
  }

  private StepEffect generateHaiku(UserInput userInput) {

    var haiku =
      componentClient.
        forAgent()
        .inSession(this.workflowId)
        .method(HaikuGenAgent::generate)
        .invoke(userInput.originalInput());

    return stepEffects()
      .updateState(currentState().withHaiku(haiku))
      .thenTransitionTo(AgentTeamWorkflow::generateImage)
      .withInput(haiku);
  }

  private StepEffect generateImage(Haiku haiku) {

    var url = imageGenerator.generateImage(haiku.formatted());

    log.info("Image generated: {}", url);

    return stepEffects()
      .updateState(currentState().withImageUrl(url))
      .thenEnd();
  }

  private StepEffect generateCensoredImage() {
    return stepEffects()
      .updateState(currentState().withImageUrl("img/censored.png"))
      .thenEnd();
  }

}
