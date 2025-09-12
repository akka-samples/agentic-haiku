package akka.haiku.storage.infrastructure;

import akka.haiku.storage.application.BlobStorage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class GCPBlobStorage implements BlobStorage {

  private static final Logger log = LoggerFactory.getLogger(GCPBlobStorage.class);
  String bucketName = "akka-haiku";
  private final Storage storage;

  public GCPBlobStorage() {
    storage = StorageOptions.getDefaultInstance().getService();
  }

  @Override
  public String uploadPng(byte[] data, String pathPrefix, String namePrefix) {

    String objectName = pathPrefix + "/" + namePrefix + UUID.randomUUID() + ".png";
    BlobId blobId = BlobId.of(bucketName, objectName);

    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();

    Blob blob = storage.create(blobInfo, data);

    String publicUrl = String.format("https://storage.googleapis.com/%s/%s",
      blob.getBucket(), blob.getName());

    log.info("Image uploaded to: gs://{}/{}", blob.getBucket(), blob.getName());
    log.info("Public url: {}", publicUrl);

    return publicUrl;
  }
}
