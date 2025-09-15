package akka.haiku.storage.application;

public interface BlobStorage {

  String uploadPng(byte[] data, String pathPrefix, String namePrefix);
}
