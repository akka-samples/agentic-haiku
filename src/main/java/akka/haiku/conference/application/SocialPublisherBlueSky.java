package akka.haiku.conference.application;

import akka.http.javadsl.model.ContentTypes;
import akka.javasdk.http.HttpClient;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SocialPublisherBlueSky implements SocialPublisher {
  private static final Logger log = LoggerFactory.getLogger(SocialPublisherBlueSky.class);


  private final Config config;
  private final HttpClient httpClient;
  private final String identifier;
  private final String password;

  record Session(String accessJwt, String did) {
  }

  public SocialPublisherBlueSky(Config config, HttpClient httpClient, String identifier, String password) {
    this.config = config;
    this.httpClient = httpClient;
    this.identifier = identifier;
    this.password = password;
  }

    @Override
    public void publish(String message, String imageUrl, List<String> tags, List<String> handlers) {
        try {
          var sess = initiateSession();
          StringBuilder post = new StringBuilder();

          // if there are handlers, we publish a best-wishes messages
          // followed by the handlers
          if (handlers != null && !handlers.isEmpty()) {
            var messages = config.getStringList("haiku.best-wishes");
            int randomIndex = new Random().nextInt(messages.size());
            post.append(messages.get(randomIndex));
            post.append("\n");
          }

          // Prepare facets for mentions and tags
          List<Map<String, Object>> facets = new java.util.ArrayList<>();

          // Add mentions to post and facets
          // only add mentions when posting from official account
          if (handlers != null && !handlers.isEmpty()) {

            for (String handler : handlers) {
              if (handler != null && !handler.isBlank()) {
                String cleanHandle = handler.replace("@", "").trim();
                // Resolve handle to DID (if not already a DID)
                String did = cleanHandle.startsWith("did:") ? cleanHandle : resolveDidForHandle(cleanHandle);
                if (did != null && !did.isBlank()) {
                  String mentionText = "@" + cleanHandle;
                  post.append(" ").append(mentionText);
                  int start = post.length() - mentionText.length();
                  int end = post.length();
                  Map<String, Object> index = new HashMap<>();
                  index.put("byteStart", start);
                  index.put("byteEnd", end);
                  Map<String, Object> feature = new HashMap<>();
                  feature.put("$type", "app.bsky.richtext.facet#mention");
                  feature.put("did", did);
                  Map<String, Object> facet = new HashMap<>();
                  facet.put("index", index);
                  facet.put("features", List.of(feature));
                  facets.add(facet);
                }
            }

            // two lines to clearly separate it from the haiku
            post.append("\n\n");
          }
        }

          post.append(message);

          // Append tags as hashtags, ensuring correct formatting and collect facets
          if (tags != null && !tags.isEmpty()) {
            post.append("\n");
            for (String tag : tags) {
              if (tag != null && !tag.isBlank()) {
                String cleanTag = tag.trim().replace("#", "").replaceAll("[^A-Za-z0-9_]+", "");
                if (!cleanTag.isEmpty()) {
                  String hashtag = "#" + cleanTag;
                  post.append(" ").append(hashtag);
                  int start = post.length() - hashtag.length();
                  int end = post.length();
                  Map<String, Object> index = new HashMap<>();
                  index.put("byteStart", start);
                  index.put("byteEnd", end);
                  Map<String, Object> feature = new HashMap<>();
                  feature.put("$type", "app.bsky.richtext.facet#tag");
                  feature.put("tag", cleanTag);
                  Map<String, Object> facet = new HashMap<>();
                  facet.put("index", index);
                  facet.put("features", List.of(feature));
                  facets.add(facet);
                }
              }
            }
          }

          log.debug("BlueSky post text: {}", post.toString());

          String now = Instant.now().toString();
          Map<String, Object> record = new HashMap<>();
          record.put("$type", "app.bsky.feed.post");
          record.put("text", post.toString());
          record.put("createdAt", now);
          if (!facets.isEmpty()) {
            record.put("facets", facets);
          }

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

  // Helper to resolve a handle to a DID using BlueSky identity service
  private String resolveDidForHandle(String handle) {
    try {
      var response = httpClient.GET("/xrpc/com.atproto.identity.resolveHandle?handle=" + handle)
        .responseBodyAs(Map.class)
        .invoke();
      var statusCode = response.httpResponse().status().intValue();
      if (statusCode >= 200 && statusCode < 300) {
        Map<?, ?> respBody = response.body();
        Object did = respBody.get("did");
        return did != null ? did.toString() : null;
      } else {
        log.warn("Failed to resolve DID for handle {}: {}", handle, response.body());
        return null;
      }
    } catch (Exception e) {
      log.error("Exception resolving DID for handle {}", handle, e);
      return null;
    }
  }
}
