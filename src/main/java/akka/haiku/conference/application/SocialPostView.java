package akka.haiku.conference.application;

import akka.haiku.conference.application.SocialPostEntity.SocialPostState;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

import java.time.Instant;
import java.util.List;

@ComponentId("social-post-view")
public class SocialPostView extends View {

  public record SocialPostRow(String id,
                              String post,
                              String url,
                              List<String> tags,
                              List<String> names,
                              List<String> bskyHandlers,
                              Instant scheduleTime) {
  }

  public record SocialPostItems(List<SocialPostRow> items) {}

     @Consume.FromKeyValueEntity(SocialPostEntity.class)
    public static class Updater extends TableUpdater<SocialPostRow> {
      public Effect<SocialPostRow> onChange(SocialPostState state) {
        var id = updateContext().eventSubject().get();
        if (state.created()) {
          var row = new SocialPostRow(
            id,
            state.post(),
            state.imageUrl(),
            state.tags(),
            state.names(),
            state.bskyHandlers(),
            state.scheduleTime());
            return effects().updateRow(row);
        } else {
            // this view exists to support the posts-queue
            // once the post is rejected, approved or published, it's removed
            return effects().deleteRow();
        }
      }
    }

    @Query(value = """
      SELECT * as items
      FROM social_post
      ORDER BY scheduleTime DESC
      LIMIT 50""")
    public QueryEffect<SocialPostItems> getAllPosts() {
        return queryResult();
    }

}
