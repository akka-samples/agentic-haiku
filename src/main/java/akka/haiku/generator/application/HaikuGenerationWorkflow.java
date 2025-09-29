package akka.haiku.generator.application;

import akka.Done;
import akka.haiku.generator.domain.ContentGeneration;
import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.UserInput;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;
import static java.time.Duration.ofSeconds;

@ComponentId("image-generation-workflow")
public class HaikuGenerationWorkflow extends Workflow<ContentGeneration> {

  private static final Logger log = LoggerFactory.getLogger(HaikuGenerationWorkflow.class);
  private final ComponentClient componentClient;
  private final String workflowId;
  private final ImageGenerator imageGenerator;
  private final double failureRate;

  public HaikuGenerationWorkflow(WorkflowContext workflowContext,
                                 ComponentClient componentClient,
                                 ImageGenerator imageGenerator,
                                 Config config) {
    this.componentClient = componentClient;
    this.workflowId = workflowContext.workflowId();
    this.imageGenerator = imageGenerator;
    this.failureRate = config.getDouble("haiku.app.force-failure-rate");
  }

  public record StartGeneration(String input) {
  }


  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepTimeout(ofSeconds(15))
      .defaultStepRecovery(maxRetries(5).failoverTo(HaikuGenerationWorkflow::timeoutStep))
      .stepTimeout(HaikuGenerationWorkflow::generateImage, ofSeconds(30))
      .stepRecovery(HaikuGenerationWorkflow::generateImage, maxRetries(3).failoverTo(HaikuGenerationWorkflow::timeoutStep))
      .build();
  }

  public Effect<Done> start(StartGeneration startGeneration) {
    if (currentState() != null) {
      log.info("Already in progress, ignoring");
      return effects().reply(done());
    } else {
      log.info("Workflow [{}]: starting image generation for input: {}", workflowId, startGeneration);

      return effects()
        .updateState(ContentGeneration.empty())
        .transitionTo(HaikuGenerationWorkflow::checkMessageQuality)
        .withInput(UserInput.of(startGeneration.input))
        .thenReply(done());
    }
  }

  public ReadOnlyEffect<ContentGeneration> getState() {
    if (currentState() != null) {
      return effects().reply(currentState());
    } else {
      throw new IllegalStateException("Current state is null");
    }
  }

  public Effect<Done> delete() {
    if (currentState() != null) {
      return effects().delete().thenReply(done());
    } else {
      throw new IllegalStateException("Current state is null");
    }
  }

  private StepEffect checkMessageQuality(UserInput userInput) {

    log.debug("Workflow [{}]: checking message quality.", workflowId);

    if (Math.random() < failureRate) {
      log.error("Workflow [{}]: random failure occurred during message quality check.", workflowId);
      throw new RuntimeException("Random failure during message quality check");
    }

    var evaluated = componentClient
      .forAgent()
      .inSession(this.workflowId)
      .method(ToxicityDetectorAgent::evaluateContent)
      .invoke(userInput);

    if (evaluated.isAccepted()) {
      log.debug("Workflow [{}]: message is accepted.", workflowId);
      return stepEffects()
        .updateState(currentState().accepted())
        .thenTransitionTo(HaikuGenerationWorkflow::analyseSentiment)
        .withInput(evaluated);

    } else {
      log.debug("Workflow [{}]: message is rejected. Flagged as {}", workflowId, evaluated.eval());
      return stepEffects()
        .updateState(currentState().toxicityDetected())
        .thenEnd();
    }
  }

  private StepEffect analyseSentiment(UserInput userInput) {

    log.debug("Workflow [{}]: analysing message sentiment.", workflowId);

    var evaluated = componentClient
      .forAgent()
      .inSession(this.workflowId)
      .method(SentimentDetectorAgent::analyseSentiment)
      .invoke(userInput);

    if (evaluated.isNegative()) {
      log.debug("Workflow [{}]: message is negative, discarding it.", workflowId);
      return stepEffects()
        .updateState(currentState().negativityDetected())
        .thenEnd();

    } else {
      log.debug("Workflow [{}]: message is positive or neutral, generating a Haiku...", workflowId);
      return stepEffects()
        .updateState(currentState().validated().withUserInput(userInput.originalInput()))
        .thenTransitionTo(HaikuGenerationWorkflow::generateHaiku)
        .withInput(evaluated);
    }
  }

  private StepEffect generateHaiku(UserInput userInput) {

    log.debug("Workflow [{}]: generating Haiku from message.", workflowId);
    var haiku = componentClient
      .forAgent()
      .inSession(this.workflowId)
      .method(HaikuGenAgent::generate)
      .invoke(userInput.originalInput());

    return stepEffects()
      .updateState(currentState().withHaiku(haiku))
      .thenTransitionTo(HaikuGenerationWorkflow::generateImage)
      .withInput(haiku);
  }

  private StepEffect generateImage(Haiku haiku) {

    log.debug("Workflow [{}]: generating image for Haiku.", workflowId);
    var url = imageGenerator.generateImage(currentState().userInput(), haiku.formatted());
    log.info("Image generated: {}", url);

    return stepEffects()
      .updateState(currentState().withImageUrl(url))
      .thenEnd();
  }

  private StepEffect timeoutStep() {
    log.debug("Workflow [{}]: finalizing with timeout image.", workflowId);
    return stepEffects()
      .updateState(currentState().timedOut())
      .thenEnd();
  }

}
