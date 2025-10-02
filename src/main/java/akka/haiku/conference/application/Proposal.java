package akka.haiku.conference.application;

import java.time.Instant;
import java.util.List;

public record Proposal(int id,
                       String title,
                       String description,
                       String summary,
                       List<Speaker> speakers,
                       List<Keyword> keywords,
                       List<ProposalTimeSlot> timeSlots
) {}

record ProposalTimeSlot(String timezone, Instant fromDate, String roomName) {}
