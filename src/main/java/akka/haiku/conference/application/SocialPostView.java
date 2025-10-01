package akka.haiku.conference.application;

import akka.haiku.conference.application.SocialPostEntity.SocialPostState;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

import java.util.List;

@ComponentId("social-post-view")
public class SocialPostView extends View {

  public record SocialPostRow(String id, String talkId, String post, String url, List<String> tags,
                              List<String> users) {
  }

     @Consume.FromKeyValueEntity(SocialPostEntity.class)
    public static class Updater extends TableUpdater<SocialPostRow> {
        public Effect<SocialPostRow> onChange(SocialPostState state) {
            var id = updateContext().eventSubject().get();
            if (!state.published() && !state.rejected()) {
              var row = new SocialPostRow(
                id, state.talkId(),
                state.post(),
                state.imageUrl(),
                state.tags(),
                state.users());
                return effects().updateRow(row);
            } else {
                return effects().deleteRow();
            }
        }
    }

    @Query(value = "SELECT * FROM social_post", streamUpdates = true)
    public QueryStreamEffect<SocialPostRow> getOpenPosts() {
        return queryStreamResult();
    }

}
