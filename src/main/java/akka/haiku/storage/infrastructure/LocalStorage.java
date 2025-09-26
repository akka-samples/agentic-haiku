package akka.haiku.storage.infrastructure;

import akka.haiku.storage.application.BlobStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalStorage implements BlobStorage {
  private static final Logger log = LoggerFactory.getLogger(LocalStorage.class);

  @Override
  public String uploadPng(byte[] data, String pathPrefix, String namePrefix) {
    try {
      Path tempFile = Files.createTempFile(namePrefix, ".png");

      Files.write(tempFile, data);

      return "file://"+tempFile.toAbsolutePath();
    } catch (IOException e) {
      log.error("Error while trying to store the PNG file", e);
      throw new RuntimeException(e);
    }
  }
}
