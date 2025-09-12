package akka.haiku.gateway.api;

import akka.haiku.application.TextInputCollectorEntity;
import akka.haiku.gateway.application.TokenGroupEntity;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.HttpCookie;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.model.headers.SetCookie;
import akka.javasdk.CommandException;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint
public class GatewayEndpoint {

  private static final Logger log = LoggerFactory.getLogger(GatewayEndpoint.class);
  private final ComponentClient componentClient;

  public record AddInputRequest(String tokenGroupId, String token, String input) {

    public boolean hasToken() {
      return token != null && !token.isBlank()
        && tokenGroupId != null && !tokenGroupId.isBlank();
    }

    public boolean hasInput() {
      return input != null && !input.isBlank();
    }
  }

  public GatewayEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/gateway/{tokenGroupId}")
  public HttpResponse redirectWithAToken(String tokenGroupId) {
    try {
      String token = componentClient.forKeyValueEntity(tokenGroupId)
        .method(TokenGroupEntity::getToken)
        .invoke();

      return HttpResponse.create()
        .withStatus(StatusCodes.FOUND)
        .addHeader(Location.create("/form"))
        .addHeader(SetCookie.create(HttpCookie.create("token", token).withPath("/")))
        .addHeader(SetCookie.create(HttpCookie.create("tokenGroupId", tokenGroupId).withPath("/")));
    } catch (CommandException e) {
      log.error(e.getMessage(), e);
      return HttpResponse.create()
        .withStatus(StatusCodes.FOUND)
        .addHeader(Location.create("/scan-qr-code"));
    }
  }

  @Post("/gateway/inputs")
  public HttpResponse addInput(AddInputRequest request) {

    if (!request.hasToken()) {
      log.debug("Rejecting, no token provided");
      return HttpResponse.create()
        .withStatus(StatusCodes.UNAUTHORIZED);
    }

    if (!request.hasInput()) {
      log.debug("Rejecting, no input provided");
      return HttpResponses.badRequest("No input provided");
    }

    var isTokenValid = componentClient.forKeyValueEntity(request.tokenGroupId)
      .method(TokenGroupEntity::isValid)
      .invoke(request.token);

    if (isTokenValid) {

      componentClient
        .forKeyValueEntity(request.tokenGroupId)
        .method(TokenGroupEntity::tokenUsed)
        .invoke(request.token);

      //TODO call the workflow directly
      componentClient
        .forEventSourcedEntity("collectorA")
        .method(TextInputCollectorEntity::addTextInput)
        .invoke(request.input);


      return HttpResponses.ok();
    } else {
      log.debug("Rejecting, invalid token provided");
      return HttpResponse.create()
        .withStatus(StatusCodes.UNAUTHORIZED);
    }
  }
}
