package com.example.infrastructure;


import com.google.api.gax.rpc.ApiException;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GenerateImageSample {

  public static void main(String[] args) throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "akka-dev-ai";
    String location = "us-central1";
    String prompt = "Create a random image of a cat playing with a ball."; // The text prompt describing what you want to see.

    generateImage(projectId, location, prompt);
  }

  // Generate an image using a text prompt using an Imagen model
  public static PredictResponse generateImage(String projectId, String location, String prompt)
    throws ApiException, IOException {
    final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
    PredictionServiceSettings predictionServiceSettings =
      PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();

    // --- Configuration: Set your bucket and desired object path ---
    String bucketName = "akka-haiku";
    String objectPathPrefix = "generated-images/"; // Example: "folder/subfolder/"

// --- Initialize the GCS client once ---
// This will use Application Default Credentials to authenticate.
    Storage storage = StorageOptions.getDefaultInstance().getService();

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests.
    try (PredictionServiceClient predictionServiceClient =
           PredictionServiceClient.create(predictionServiceSettings)) {

      final EndpointName endpointName =
        EndpointName.ofProjectLocationPublisherModelName(
          projectId, location, "google", "imagen-3.0-generate-001");

      Map<String, Object> instancesMap = new HashMap<>();
      instancesMap.put("prompt", prompt);
      Value instances = mapToValue(instancesMap);

      Map<String, Object> paramsMap = new HashMap<>();
      paramsMap.put("sampleCount", 1);
      // You can't use a seed value and watermark at the same time.
      // paramsMap.put("seed", 100);
      // paramsMap.put("addWatermark", false);
      paramsMap.put("aspectRatio", "1:1");
      paramsMap.put("safetyFilterLevel", "block_some");
      paramsMap.put("personGeneration", "allow_adult");
      Value parameters = mapToValue(paramsMap);

      PredictResponse predictResponse =
        predictionServiceClient.predict(
          endpointName, Collections.singletonList(instances), parameters);

      for (Value prediction : predictResponse.getPredictionsList()) {
        Map<String, Value> fieldsMap = prediction.getStructValue().getFieldsMap();
        if (fieldsMap.containsKey("bytesBase64Encoded")) {
          String bytesBase64Encoded = fieldsMap.get("bytesBase64Encoded").getStringValue();
          Path tmpPath = Files.createTempFile("imagen-", ".png");
          Files.write(tmpPath, Base64.getDecoder().decode(bytesBase64Encoded));
          System.out.format("Image file written to: %s\n", tmpPath.toUri());


          byte[] imageBytes = Base64.getDecoder().decode(bytesBase64Encoded);
          // 2. Define the destination object in GCS
          // We use a UUID to ensure the filename is always unique.
          String objectName = objectPathPrefix + "imagen-" + UUID.randomUUID().toString() + ".png";
          BlobId blobId = BlobId.of(bucketName, objectName);

          // 3. Set metadata for the object (e.g., content type)
          BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();

          try {
            // 4. Upload the bytes to GCS
            Blob blob = storage.create(blobInfo, imageBytes);

            // --- GET THE PUBLIC URL ---
            // The public URL for a GCS object follows a standard format.
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s",
              blob.getBucket(), blob.getName());

            System.out.format("Image uploaded to: gs://%s/%s\n", blob.getBucket(), blob.getName());
            System.out.format("Public URL: %s\n", publicUrl);

          } catch (Exception e) {
            System.err.println("Error uploading to GCS: " + e.getMessage());
            // Handle the exception as needed
          }
        }
      }
      return predictResponse;
    }
  }

  private static Value mapToValue(Map<String, Object> map) throws InvalidProtocolBufferException {
    Gson gson = new Gson();
    String json = gson.toJson(map);
    Value.Builder builder = Value.newBuilder();
    JsonFormat.parser().merge(json, builder);
    return builder.build();
  }
}
