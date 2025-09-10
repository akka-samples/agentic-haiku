package akka.haiku.application;

import akka.Done;
import akka.haiku.domain.ContentGeneration;
import akka.haiku.domain.Haiku;
import akka.haiku.domain.UserInput;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static akka.Done.done;

@ComponentId("image-generation-workflow")
public class ContentGenerationWorkflow extends Workflow<ContentGeneration> {

  private static final Logger log = LoggerFactory.getLogger(ContentGenerationWorkflow.class);
  private final ComponentClient componentClient;
  private final String workflowId;
  private final ImageGenerator imageGenerator;

  public ContentGenerationWorkflow(WorkflowContext workflowContext, ComponentClient componentClient, ImageGenerator imageGenerator) {
    this.componentClient = componentClient;
    this.workflowId =workflowContext.workflowId();
    this.imageGenerator = imageGenerator;
  }

  public record StartGeneration(String inputId, String input) {
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
      log.info("Starting image generation with inputs: {}", startGeneration);
      ContentGeneration contentGeneration = ContentGeneration.of(startGeneration.inputId, startGeneration.input);
      return effects().updateState(contentGeneration)
        .transitionTo(ContentGenerationWorkflow::checkForHarmfulContent)
        .withInput(UserInput.of(startGeneration.input))
        .thenReply(done());
    }
  }

  private StepEffect checkForHarmfulContent(UserInput userInput) {
    var evaluated =
      componentClient
        .forAgent()
        .inSession(this.workflowId)
        .method(HarmfulContentDetectorAgent::evaluate)
        .invoke(userInput);

    if (evaluated.isAccepted()) {
      log.debug("Content is accepted.");
      return stepEffects()
        .thenTransitionTo(ContentGenerationWorkflow::analyseSentiment)
        .withInput(evaluated);

    } else {
      log.debug("Content is rejected, discarding it.");
      return stepEffects()
        .thenTransitionTo(ContentGenerationWorkflow::generateCensoredImage);
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
        .thenTransitionTo(ContentGenerationWorkflow::generateCensoredImage);

    } else {
      log.debug("Content is positive or neutral, generating a Haiku...");
      return stepEffects()
        .thenTransitionTo(ContentGenerationWorkflow::generateHaiku)
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
      .thenTransitionTo(ContentGenerationWorkflow::generateImage)
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

    // TODO: use a fixed image
    var url = imageGenerator.generateImage("Generate an image of a warning icon or symbol that clearly represents censored or restricted content.");

    log.info("Censored image generated: {}", url);

    return stepEffects()
      .updateState(currentState().withImageUrl(url))
      .thenEnd();
  }

}
