package akka.haiku.conference.api;

import akka.haiku.conference.application.ScheduleScanner;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;

@HttpEndpoint("/schedule")
public record ScheduleEndpoint(ScheduleScanner scanner) {

  @Get("/scan/{day}")
  public void scan(String day) {
    scanner.scheduleTimersFor(day);
  }

  @Get("/now/{proposalId}")
  public void scheduleNow(int proposalId) {
    scanner.scheduleNow(proposalId);
  }
}
