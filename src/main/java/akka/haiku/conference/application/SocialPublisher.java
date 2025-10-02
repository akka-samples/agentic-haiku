package akka.haiku.conference.application;

import java.util.List;

public interface SocialPublisher {

  void publish(String message, String imageUrl, List<String> tags, List<String> handlers);


}
