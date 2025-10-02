package akka.haiku.conference.application;

import akka.http.javadsl.model.ContentTypes;
import akka.javasdk.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialPublisherBlueSky implements SocialPublisher {
  private static final Logger log = LoggerFactory.getLogger(SocialPublisherBlueSky.class);


  private final HttpClient httpClient;
  private final String identifier;
  private final String password;

  record Session(String accessJwt, String did) {
  }

  public SocialPublisherBlueSky(HttpClient httpClient, String identifier, String password) {
    this.httpClient = httpClient;
    this.identifier = identifier;
    this.password = password;
  }

    @Override
    public void publish(String message, String imageUrl, List<String> tags, List<String> handlers) {
        try {
          var sess = initiateSession();

          StringBuilder post = new StringBuilder(message);

          // only tag or add mentions when posting from official account
          if (identifier.equals("akka.io")) {
            if (tags != null && !tags.isEmpty()) {
              for (String tag : tags) {
                post.append(" #").append(tag.replace("#", ""));
              }
            }
            if (handlers != null && !handlers.isEmpty()) {
              for (String handler : handlers) {
                post.append(" @").append(handler.replace("@", ""));
              }
            }
          }

          String now = Instant.now().toString();
          Map<String, Object> record = new HashMap<>();
          record.put("$type", "app.bsky.feed.post");
          record.put("text", post.toString());
          record.put("createdAt", now);

          // Image upload logic
          if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
              // Download image bytes
              java.net.URL url = new java.net.URL(imageUrl);
              byte[] imageBytes;
              try (java.io.InputStream in = url.openStream(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) {
                  out.write(buffer, 0, n);
                }
                imageBytes = out.toByteArray();
              }
              // Upload to BlueSky
              var uploadResp =
                httpClient
                  .POST("/xrpc/com.atproto.repo.uploadBlob")
                  .addHeader("Authorization", "Bearer " + sess.accessJwt)
                  .withRequestBody(ContentTypes.parse("image/jpg"), imageBytes)
                  .responseBodyAs(Map.class)
                  .invoke();

              var statusCode = uploadResp.httpResponse().status().intValue();

              if (statusCode >= 200 && statusCode < 300) {

                Map<?, ?> respBody = uploadResp.body();
                log.info(respBody.toString());
                var blob = (Map<?, ?>) respBody.get("blob");
                if (blob != null) {
                  Map<String, Object> imageEmbed = new HashMap<>();
                  imageEmbed.put("$type", "app.bsky.embed.images");
                  Map<String, Object> imageObj = new HashMap<>();
                  imageObj.put("image", blob); // Pass the full blob object
                  imageObj.put("alt", post.toString()); // Use the haiku/message as alt text
                  imageEmbed.put("images", List.of(imageObj));
                  record.put("embed", imageEmbed);
                } else {
                  log.warn("No blob found in upload response");
                }
              } else {
                log.error("Failed to upload image to BlueSky. Status: {}, Body: {}", statusCode, uploadResp.body());
              }
            } catch (Exception e) {
              log.error("Exception uploading image to BlueSky", e);
            }
          }

          Map<String, Object> body = new HashMap<>();
          body.put("collection", "app.bsky.feed.post");
          body.put("repo", sess.did);
          body.put("record", record);

          log.debug("bsky post body: {}", body);
          var response =
            httpClient
              .POST("/xrpc/com.atproto.repo.createRecord")
              .addHeader("Authorization", "Bearer " + sess.accessJwt)
              .withRequestBody(body)
              .responseBodyAs(String.class)
              .invoke();

          var statusCode = response.httpResponse().status().intValue();
          if (statusCode >= 200 && statusCode < 300) {
            log.info("BlueSky post published: {}", response.body());
          } else {
            log.error("Failed to publish to BlueSky. Status: {}, Body: {}", statusCode, response.body());
          }
        } catch (Exception e) {
          log.error("Exception while publishing to BlueSky", e);
        }
    }

  private Session initiateSession() {
    Map<String, String> body = new HashMap<>();
    body.put("identifier", identifier);
    body.put("password", password);

    var response = httpClient.POST("/xrpc/com.atproto.server.createSession")
      .withRequestBody(body)
      .responseBodyAs(Map.class)
      .invoke();

    var statusCode = response.httpResponse().status().intValue();

    if (statusCode >= 200 && statusCode < 300) {
      Map<String, String> respBody = response.body();
      return new Session(respBody.get("accessJwt"), respBody.get("did"));
    } else {
      throw new RuntimeException("Failed to authenticate with BlueSky: " + response.body());
    }
  }
}
