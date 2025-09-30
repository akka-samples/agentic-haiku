package akka.haiku.conference.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import akka.javasdk.keyvalueentity.KeyValueEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ComponentId("social-post-entity")
public class SocialPostEntity extends KeyValueEntity<SocialPostEntity.SocialPostState> {

    private static final Logger log = LoggerFactory.getLogger(SocialPostEntity.class);


  public record SocialPostState(String post,
                                String imageUrl,
                                List<String> tags,
                                List<String> users,
                                boolean published) {

      public static SocialPostState of(String post, String imageUrl, List<String> tags) {
        return new SocialPostState(post, imageUrl,  tags, List.of(), false);
      }

      public SocialPostState withUsers(List<String> users) {
        return new SocialPostState(post, imageUrl,  tags, users, published);
      }
  }

    public Effect<Done> createPost(SocialPostState post) {
        log.info("Creating social post {}", commandContext().entityId());
        // we allow updates, this allows us to repost it
        return effects().updateState(post).thenReply(Done.done());
    }

    public Effect<Done> publish() {
        SocialPostState state = currentState();
        if (state == null) {
            return effects().error("Post not found: " + commandContext().entityId());
        } else if (state.published()) {
            return effects().reply(Done.done());
        } else {
            SocialPostState updated = new SocialPostState(state.post(), state.imageUrl, state.tags(), state.users(), true);
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

}
