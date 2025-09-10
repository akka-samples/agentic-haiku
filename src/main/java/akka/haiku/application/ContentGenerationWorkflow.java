package akka.haiku.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import akka.haiku.domain.ContentGeneration;
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
        .transitionTo(ContentGenerationWorkflow::generateImage)
        .thenReply(done());
    }
  }

  private StepEffect generateImage(){

    var url = imageGenerator.generateImage(currentState().prompt());

    log.info("Image generated: {}", url);

    return stepEffects()
      .updateState(currentState().withImageUrl(url))
      .thenEnd();
  }

}
