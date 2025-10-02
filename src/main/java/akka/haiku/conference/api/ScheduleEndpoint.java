package akka.haiku.conference.api;

import akka.haiku.conference.application.ScheduleScanner;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;

@HttpEndpoint("/schedule")
public record ScheduleEndpoint(ScheduleScanner scanner) {

  @Post("/scan/{day}")
  public void scan(String day) {
    scanner.scheduleTimersFor(day);
  }

  @Post("/now/{proposalId}")
  public void scheduleNow(int proposalId) {
    scanner.scheduleNow(proposalId);
  }
}
