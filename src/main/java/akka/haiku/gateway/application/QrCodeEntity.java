package akka.haiku.gateway.application;

import akka.Done;
import akka.haiku.gateway.domain.QrCode;
import akka.javasdk.annotations.Component;
import akka.javasdk.keyvalueentity.KeyValueEntity;

import static akka.Done.done;

@Component(id = "qr-code")
public class QrCodeEntity extends KeyValueEntity<QrCode> {

  public Effect<Done> create(String qrCodeUrl) {
    if (currentState() != null) {
      return effects().reply(done());
    } else {
      var tokenGroupId = commandContext().entityId();
      return effects()
        .updateState(new QrCode(tokenGroupId, qrCodeUrl))
        .thenReply(done());
    }
  }
}
