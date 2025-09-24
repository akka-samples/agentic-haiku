package akka.haiku.gateway.application;

import akka.haiku.storage.application.BlobStorage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.typesafe.config.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class QrCodeGenerator {

  private final BlobStorage blobStorage;
  private final Config config;

  public QrCodeGenerator(BlobStorage blobStorage, Config config) {
    this.blobStorage = blobStorage;
    this.config = config;
  }

  public String generate(String tokenGroupId) {
    var urlForQrCode = ofNullable(config.getString("haiku.app.url"))
      .map(url -> url + "/gateway/" + tokenGroupId)
      .orElse("http://localhost:9000/gateway/" + tokenGroupId);
    var qrCodeImage = generateQRCodeImage(urlForQrCode, 300);
    return blobStorage.uploadPng(qrCodeImage, "qrcodes", "qrcode-");
  }

  private byte[] generateQRCodeImage(String text, int size) {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();

      // Optional settings: error correction, margin, etc.
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
      hints.put(EncodeHintType.MARGIN, 2);

      int width = size;
      int height = size;
      BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

      ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

      return pngOutputStream.toByteArray();
    } catch (WriterException | IOException e) {
      throw new RuntimeException("Error when generating QR Code image", e);
    }
  }
}
