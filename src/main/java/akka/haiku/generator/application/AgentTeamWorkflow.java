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

import static akka.Done.done;
import static java.time.Duration.ofSeconds;

@ComponentId("image-generation-workflow")
public class AgentTeamWorkflow extends Workflow<ContentGeneration> {

  private static final Logger log = LoggerFactory.getLogger(AgentTeamWorkflow.class);
  private final ComponentClient componentClient;
  private final String workflowId;
  private final ImageGenerator imageGenerator;

  public AgentTeamWorkflow(WorkflowContext workflowContext,
                           ComponentClient componentClient,
                           ImageGenerator imageGenerator) {
    this.componentClient = componentClient;
    this.workflowId =workflowContext.workflowId();
    this.imageGenerator = imageGenerator;
  }

  public record StartGeneration(String input) {
  }



  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepTimeout(ofSeconds(15))
      .defaultStepRecovery(maxRetries(5).failoverTo(AgentTeamWorkflow::timeoutStep))
      .stepTimeout(AgentTeamWorkflow::generateImage, ofSeconds(30))
      .stepRecovery(AgentTeamWorkflow::generateImage, maxRetries(3).failoverTo(AgentTeamWorkflow::timeoutStep))
      .build();
  }

  public Effect<Done> start(StartGeneration startGeneration) {
    if (currentState() != null) {
      log.info("Already in progress, ignoring");
      return effects().reply(done());
    } else {
      log.info("Starting image generation for input: {}", startGeneration);

      return effects()
        .updateState(
          ContentGeneration.empty().addProgressLine("Verifying message quality"))
        .transitionTo(AgentTeamWorkflow::checkForToxicContent)
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

  private StepEffect checkForToxicContent(UserInput userInput) {

    var evaluated =
      componentClient
        .forAgent()
        .inSession(this.workflowId)
        .method(ToxicityDetectorAgent::evaluateContent)
        .invoke(userInput);

    if (evaluated.isAccepted()) {
      log.debug("Message is accepted.");
      return stepEffects()
        .updateState(currentState()
          .addProgressLine("Message is accepted.")
          .addProgressLine("Analysing message sentiment."))
        .thenTransitionTo(AgentTeamWorkflow::analyseSentiment)
        .withInput(evaluated);

    } else {
      log.debug("Message is rejected.");
      return stepEffects()
        .updateState(currentState()
          .addProgressLine("Message flagged as '" + evaluated.eval() + "'. Message is rejected."))
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
      log.debug("Message is negative, discarding it.");
      return stepEffects()
        .updateState(currentState()
          .addProgressLine("Message flagged as '" + evaluated.eval() + "'. Message is rejected."))
        .thenTransitionTo(AgentTeamWorkflow::generateCensoredImage);

    } else {
      log.debug("Message is positive or neutral, generating a Haiku...");
      return stepEffects()
        .updateState(currentState()
          .addProgressLine("Message flagged as '" + evaluated.eval() + "', generating a Haiku.")
          .withUserInput(userInput.originalInput()))
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
      .updateState(currentState()
        .addProgressLine("Haiku generated.")
        .addProgressLine("Generating Haiku image.")
        .withHaiku(haiku)
        .withGeneratedTime()
      )
      .thenTransitionTo(AgentTeamWorkflow::generateImage)
      .withInput(haiku);
  }

  private StepEffect generateImage(Haiku haiku) {

    var url = imageGenerator.generateImage(haiku.formatted());

    log.info("Image generated: {}", url);

    return stepEffects()
      .updateState(
        currentState()
          .addProgressLine("Haiku image generated.")
          .withImageUrl(url))
      .thenEnd();
  }

  private StepEffect generateCensoredImage() {
    return stepEffects()
      .updateState(
        currentState()
        .addProgressLine("Returning image for rejected message.")
          .withImageUrl("static/img/censored.png")
          .withGeneratedTime())
      .thenEnd();
  }

  private StepEffect timeoutStep() {
    return stepEffects()
      .updateState(
        currentState()
        .addProgressLine("Cancelling image generation due to timeout.")
          .withImageUrl("static/img/time-is-up.png")
          .withGeneratedTime())
      .thenEnd();
  }

}
