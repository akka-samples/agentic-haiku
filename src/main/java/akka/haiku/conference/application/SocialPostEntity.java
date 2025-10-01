package akka.haiku.conference.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ComponentId("social-post-entity")
public class SocialPostEntity extends KeyValueEntity<SocialPostEntity.SocialPostState> {

    private static final Logger log = LoggerFactory.getLogger(SocialPostEntity.class);


  public record SocialPostState(
                                String post,
                                String imageUrl,
                                List<String> tags,
                                List<String> xHandlers,
                                List<String> bskyHandlers,
                                boolean rejected,
                                boolean published) {

    public static SocialPostState of(String post, String imageUrl, List<String> tags,
                                     List<String> xHandlers,
                                     List<String> bskyHandlers) {
      return new SocialPostState(post, imageUrl, tags, xHandlers, bskyHandlers, false, false);
      }


      public SocialPostState asRejected() {
        return new SocialPostState(post, imageUrl, tags, xHandlers, bskyHandlers, true, published);
      }

    public SocialPostState asPublished() {
      return new SocialPostState(post, imageUrl, tags, xHandlers, bskyHandlers, rejected, true);
    }
  }

    public Effect<Done> createPost(SocialPostState post) {
        log.info("Creating social post {}", commandContext().entityId());
        // we allow updates, this allows us to repost it
        return effects().updateState(post).thenReply(Done.done());
    }

    public Effect<Done> approvePost() {
        SocialPostState state = currentState();
        if (state == null) {
            return effects().error("Post not found: " + commandContext().entityId());
        } else if (state.published()) {
            return effects().reply(Done.done());
        } else {
            SocialPostState updated = state.asPublished();
            log.info("Post published {}", commandContext().entityId());
            return effects().updateState(updated).thenReply(Done.done());
        }
    }

    public Effect<SocialPostState> getPost() {
        SocialPostState state = currentState();
        if (state == null) {
            return effects().error("Post not found: " + commandContext().entityId());
        } else {
            return effects().reply(state);
        }
    }

    public Effect<Done> rejectPost() {
      return effects()
        .updateState(currentState().asRejected())
        .thenReply(Done.done());
    }

}
