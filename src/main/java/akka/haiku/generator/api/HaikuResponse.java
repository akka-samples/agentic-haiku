package akka.haiku.generator.api;

import akka.haiku.generator.domain.Haiku;
import akka.haiku.generator.domain.Image;

import java.util.Optional;

public record HaikuResponse(String id,
                            String prompt,
                            Long generatedAt,
                            Optional<Haiku> haiku,
                            Optional<Image> image) {
}
