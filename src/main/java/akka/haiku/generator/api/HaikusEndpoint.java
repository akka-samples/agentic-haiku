package akka.haiku.generator.api;

import akka.haiku.generator.application.HaikuGenerationWorkflow;
import akka.haiku.generator.application.GenerationProgressView;
import akka.haiku.generator.application.HaikuView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Put;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered,
// and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/haikus")
public record HaikusEndpoint(ComponentClient componentClient, Materializer materializer) {

  private static final Logger log = LoggerFactory.getLogger(HaikusEndpoint.class);

  public record Input(String message) {}

  @Get("/{haikuId}")
  public HaikuApiModel getHaiku(String haikuId) {

    var state =
      componentClient.forWorkflow(haikuId)
        .method(HaikuGenerationWorkflow::getState)
        .invoke();

    return new HaikuApiModel(
      haikuId,
      state.userInput(),
      state.generatedAt(),
      state.haiku(),
      state.image());
  }

  @Get("/{haikuId}/progress")
  public HttpResponse getHaikuGenProgress(String haikuId) {

    var queueAndSrc =
      Source
        .<String>queue(20).preMaterialize(materializer);

    var queue = queueAndSrc.first();
    var src = queueAndSrc.second();

    AtomicReference<Integer> publishedIndex = new AtomicReference<>(0);

    // a tick source to poll the workflow state
    // workflow progress messages are pushed to a queue and sent as SSE to UI
    Source.tick(ofMillis(500), ofSeconds(1), "tick").map(t -> {
      log.debug("polling haiku gen state: {}", haikuId);

      var state =
        componentClient.forView()
          .method(GenerationProgressView::get)
          .invoke(haikuId);

      var progressMessages = state.lines();
      var size = progressMessages.size();

      int publishedCount = publishedIndex.get();

      if (publishedCount < size) {
        var messagesToPublish = progressMessages.subList(publishedCount, size);
        // we publish one by one
        queue.offer(messagesToPublish.getFirst());
        publishedIndex.set(publishedCount + 1);
      }

      // completed and we published all messages?
      if (state.completed() && publishedCount == size) {
        log.debug("closing queue");
        queue.complete();
      }

        return t;
      }
    ).run(materializer);

    return HttpResponses.serverSentEvents(src);
  }

  @Get
  public HttpResponse realTimeContent() {
    var contentUpdates = componentClient.forView()
      .stream(HaikuView::get)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }
}
