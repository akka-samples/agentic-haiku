package akka.haiku.conference.application;

import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpClient;
import akka.javasdk.timer.TimerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    var now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    AtomicInteger atomicDelay = new AtomicInteger(0);

    scheduleSlots.stream()
      .filter(scheduleSlot -> scheduleSlot.proposal() != null)
      .filter(scheduleSlot -> {
          // overflow slots have a slug set to overflow, we need to filter it out
          // for the record, there is 'overflow' bool, but it's always false
          if (scheduleSlot.sessionType() != null && scheduleSlot.sessionType().slug() != null) {
            return !scheduleSlot.sessionType().slug().equals("overflow");
          }
          return true;
        }
      )
      .forEach(scheduleSlot -> {
        log.debug("Processing schedule slot: {}, talk {}, {}", scheduleSlot.id(), scheduleSlot.proposal().id(), scheduleSlot.proposal().title());

        var delay = atomicDelay.get();
        var durationUntilGeneration = now.plus(Duration.ofSeconds(atomicDelay.get()));
        atomicDelay.set(delay + 10);

        String timerName = timerName(scheduleSlot.proposal().id());

        // we need some throttling when generate haikus for a whole day
        // if we run then all together, the LLM provider will rate-limit us
        var timeToSchedule = Duration.between(now, durationUntilGeneration);
        timerScheduler.createSingleTimer(
          timerName,
          timeToSchedule,
          componentClient.forWorkflow(String.valueOf(scheduleSlot.proposal().id()))
            .method(TalkHaikuGenerationWorkflow::start)
            .deferred()
        );

        log.info("Scheduled timer '{}' to generate haiku for  for talk '{}'",
          timerName,
          scheduleSlot.proposal().id());
      });
  }

  private String timerName(int proposalId) {
    return "talk-" + proposalId;
  }


  public void scheduleNow(int proposalId) {
    log.info("Generating Haiku for {}", proposalId);
    componentClient.forWorkflow(String.valueOf(proposalId))
      .method(TalkHaikuGenerationWorkflow::start)
      .invoke();
  }
}
