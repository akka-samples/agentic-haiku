package akka.haiku.conference.application;

import javax.annotation.Nullable;
import java.time.Instant;

public record ScheduleSlot(int id, Instant fromDate, Instant toDate, @Nullable Proposal proposal) {
}