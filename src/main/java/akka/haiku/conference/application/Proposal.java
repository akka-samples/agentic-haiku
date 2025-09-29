package akka.haiku.conference.application;

import java.util.List;

public record Proposal(int id,
                       String title,
                       String description,
                       String summary,
                       List<Speaker> speakers,
                       List<Keyword> keywords
) {}
