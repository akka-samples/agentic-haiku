package akka.haiku.conference.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;

@ComponentId("run-content-generation-for-talk")
public class ContentGenerationForTalkWorkflow extends Workflow<ContentGenerationForTalkWorkflow.ContentGenerationForTalk> {

  private final String talkId;

  public ContentGenerationForTalkWorkflow(WorkflowContext workflowContext){
    this.talkId = workflowContext.workflowId();
  }

  record ContentGenerationForTalk(String talkId) {
  }

  @Override
  public ContentGenerationForTalk emptyState() {
    return new ContentGenerationForTalk(talkId);
  }

  Effect<Done> start() {
    // This is just a temporary action to trigger content generation for a talk.
    // In the future, this will be replaced by a proper workflow that handles the entire lifecycle of content generation.
    return effects().reply(Done.done());
  }
}
