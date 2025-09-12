package akka.haiku.gateway.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.http.HttpResponses;

@HttpEndpoint
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
public class StaticResourcesEndpoint {

  @Get("/")
  public HttpResponse index() {
    return HttpResponses.staticResource("index.html");
  }

  @Get("/img/censored.png")
  public HttpResponse censoredImage() {
    return HttpResponses.staticResource("/img/censored.png");
  }

  @Get("/form")
  public HttpResponse form() {
    return HttpResponses.staticResource("form.html");
  }

  @Get("/scan-qr-code")
  public HttpResponse scanQrCode() {
    return HttpResponses.staticResource("scan-qr-code.html");
  }
}
