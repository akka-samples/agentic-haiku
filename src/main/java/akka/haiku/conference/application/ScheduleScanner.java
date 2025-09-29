package akka.haiku.conference.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpClient;
import akka.javasdk.http.HttpClientProvider;
import akka.javasdk.timedaction.TimedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ComponentId("schedule-scanner")
public class ScheduleScanner extends TimedAction {

  private static final Logger log = LoggerFactory.getLogger(ScheduleScanner.class);
  private final HttpClient httpClient;
  private final ComponentClient componentClient;
  private final List<String> DAYS = List.of("wednesday", "thursday", "friday");
  private final Duration DELAY = Duration.ofHours(1);

  public ScheduleScanner(HttpClientProvider httpClientProvider, ComponentClient componentClient) {
    this.httpClient = httpClientProvider.httpClientFor("https://dvbe25.cfp.dev");
    this.componentClient = componentClient;
  }

  public Effect scanConference() {
    DAYS.forEach(day -> scheduleTimersFor(day, DELAY));
    return effects().done();
  }

  private void scheduleTimersFor(String day, Duration delay) {
    List<ScheduleSlot> scheduleSlots = httpClient.GET("/api/public/schedules/wednesday")
      .responseBodyAsListOf(ScheduleSlot.class)
      .invoke()
      .body();

    log.info("loaded {} schedule slots for day {}", scheduleSlots.size(), day);

    scheduleSlots.stream()
      .filter(scheduleSlot -> scheduleSlot.proposal() != null)
      .forEach(scheduleSlot -> {
        log.debug("Processing schedule slot: {}, talk {}, {}", scheduleSlot.id(), scheduleSlot.proposal().id(), scheduleSlot.proposal().title());

        ZoneId belgiumZone = ZoneId.of("Europe/Brussels");
        ZonedDateTime belgiumTime = scheduleSlot.fromDate().atZone(belgiumZone);

        var now = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        var durationUntilTalk = Duration.between(now, belgiumTime.toInstant());
        var durationUntilGeneration = durationUntilTalk.minus(delay);

        String timerName = "talk-" + day + "-" + scheduleSlot.id() + "-" + belgiumTime.toLocalTime();

        timers().createSingleTimer(
          timerName,
          durationUntilGeneration,
          componentClient.forWorkflow(String.valueOf(scheduleSlot.proposal().id()))
            .method(ContentGenerationForTalkWorkflow::start)
            .deferred()
        );

        log.info("Scheduled timer '{}' to start in {} at {} for talk '{}'",
          timerName,
          durationUntilGeneration,
          now.plus(durationUntilGeneration).atZone(belgiumZone).toLocalTime(),
          scheduleSlot.proposal().id());
      });
  }

}
