package akka.haiku.conference.application;

import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpClient;
import akka.javasdk.timer.TimerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ScheduleScanner {

  private static final Logger log = LoggerFactory.getLogger(ScheduleScanner.class);
  private final HttpClient httpClient;
  private final ComponentClient componentClient;
  private final TimerScheduler timerScheduler;

  private final Duration DELAY = Duration.ofMinutes(10);

  public ScheduleScanner(HttpClient httpClient,
                         TimerScheduler timerScheduler,
                         ComponentClient componentClient) {
    this.timerScheduler = timerScheduler;
    this.httpClient = httpClient;
    this.componentClient = componentClient;
  }

  public void scheduleTimersFor(String day) {
    log.info("Scheduling timer for day {}", day);
    List<ScheduleSlot> scheduleSlots = httpClient.GET("/api/public/schedules/" + day)
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
        var durationUntilGeneration = durationUntilTalk.minus(DELAY);

        String timerName = timerName(scheduleSlot.proposal().id());

        timerScheduler.createSingleTimer(
          timerName,
          durationUntilGeneration,
          componentClient.forWorkflow(String.valueOf(scheduleSlot.proposal().id()))
            .method(TalkHaikuGenerationWorkflow::start)
            .deferred()
        );

        log.info("Scheduled timer '{}' to start in {} at {} for talk '{}'",
          timerName,
          durationUntilGeneration,
          now.plus(durationUntilGeneration).atZone(belgiumZone).toLocalTime(),
          scheduleSlot.proposal().id());
      });
  }

  private String timerName(int proposalId) {
    return "talk-" + proposalId;
  }


  public void scheduleNow(int proposalId) {
    String timerName = timerName(proposalId);
    timerScheduler.createSingleTimer(
      timerName,
      Duration.ofSeconds(0),
      componentClient.forWorkflow(String.valueOf(proposalId))
        .method(TalkHaikuGenerationWorkflow::start)
        .deferred()
    );


  }
}
