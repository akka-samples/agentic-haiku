package akka.haiku.generator.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record HaikuId(String id) {


  private static final String talkPrefix = "talk:";
  private static final String tokenPrefix = "token:";

  public static HaikuId of(String id) {
    if (id.startsWith(talkPrefix) || id.startsWith(tokenPrefix)) {
      return new HaikuId(id);
    } else
      throw new IllegalArgumentException("Invalid id: " + id);
  }

  public static HaikuId forTalk(String talkId) {
    return new HaikuId(talkPrefix + talkId + ":" + UUID.randomUUID().toString());
  }

  public static HaikuId forToken(String token) {
    return new HaikuId(tokenPrefix + token);
  }

  @JsonIgnore
  public boolean isTalk() {
    return id.startsWith(talkPrefix);
  }


  public String extractTalkId() {
    if (id.startsWith(talkPrefix)) {
      String[] parts = id.split(":");
      return parts[1];
    } else  {
      throw new IllegalStateException("Current haiku id isn't a talk");
    }
  }

}
