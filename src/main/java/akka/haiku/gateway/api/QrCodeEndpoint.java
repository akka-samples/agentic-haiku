package akka.haiku.gateway.api;

import akka.haiku.gateway.application.QrCodeView;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint
public class QrCodeEndpoint {

  private final ComponentClient componentClient;

  public QrCodeEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/qrcodes")
  public HttpResponse qrCodes() {
    var contentUpdates = componentClient.forView()
      .stream(QrCodeView::get)
      .source();

    return HttpResponses.serverSentEvents(contentUpdates);
  }
}
