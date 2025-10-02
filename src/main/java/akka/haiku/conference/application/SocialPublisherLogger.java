package akka.haiku.conference.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SocialPublisherLogger implements SocialPublisher {

  private final Logger log = LoggerFactory.getLogger(SocialPublisherLogger.class);

  @Override
  public void publish(String message, String imageUrl, List<String> tags, List<String> handlers) {

    log.debug("'Publishing' on [{}], tags [{}], handlers [{}]", message, tags, handlers);
  }
}
