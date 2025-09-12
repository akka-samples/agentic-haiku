package akka.haiku.generator.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ContentGeneration(String userInput,
                                Long generatedAt,
                                List<String> progress,
                                Optional<Haiku> haiku,
                                Optional<Image> image) {

  public static ContentGeneration empty() {
    return new ContentGeneration("***********", null, List.of(), Optional.empty(), Optional.empty());
  }

  public ContentGeneration withImageUrl(String imageUrl) {
    return new ContentGeneration(userInput, Instant.now().toEpochMilli(), progress, haiku, Optional.of(new Image(imageUrl)));
  }

  public ContentGeneration withUserInput(String userInput) {
    return new ContentGeneration(userInput, generatedAt, progress, haiku, image) ;
  }

  public ContentGeneration withHaiku(Haiku haiku) {
    return new ContentGeneration(userInput, generatedAt, progress, Optional.of(haiku), image);
  }

  public ContentGeneration addProgressLine(String line) {
    var newList = new ArrayList<>(progress);
    newList.add(line);
    List<String> immutableList = List.copyOf(newList);
    return new ContentGeneration(userInput, generatedAt, immutableList, haiku, image);
  }

  public boolean isComplete() {
    return image.isPresent();
  }
}
