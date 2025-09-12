package akka.haiku.gateway.application;

import akka.haiku.gateway.domain.QrCode;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;

@ComponentId("qr-code-view")
public class QrCodeView extends View {

  @Consume.FromKeyValueEntity(QrCodeEntity.class)
  public static class QrCodeUpdater extends TableUpdater<QrCode> {
  }

  @Query(value = "SELECT * from qr_codes", streamUpdates = true)
  public QueryStreamEffect<QrCode> get() {
    return queryStreamResult();
  }
}
