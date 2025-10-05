package akka.haiku.conference.application;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class SocialPublisherLogger implements SocialPublisher {

  private final Config config;;
  private final Logger log = LoggerFactory.getLogger(SocialPublisherLogger.class);

  public SocialPublisherLogger(Config config) {
    this.config = config;
  }

  @Override
  public void publish(String message, String imageUrl, List<String> tags, List<String> names, List<String> handlers) {

    var separator = "----------------------------------------\n";
    StringBuilder stringBuilder = new StringBuilder(separator);


    names.forEach(name -> stringBuilder.append(name).append("\n"));
    handlers.forEach(handler -> stringBuilder.append(handler).append(", "));

    if (!names.isEmpty() || !handlers.isEmpty()) {
      var messages = config.getStringList("haiku.best-wishes");
      int randomIndex = new Random().nextInt(messages.size());
      stringBuilder.append(messages.get(randomIndex));
      stringBuilder.append("\n\n");
    }

    stringBuilder.append(message);
    stringBuilder.append("\n");

    tags.forEach(tag -> stringBuilder.append("#").append(tag).append(" "));
    stringBuilder.append("\n\n");

    stringBuilder.append(imageUrl);
    stringBuilder.append("\n");
    stringBuilder.append(separator);

    log.info(stringBuilder.toString());


  }
}
