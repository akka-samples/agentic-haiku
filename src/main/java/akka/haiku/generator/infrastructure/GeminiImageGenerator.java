package akka.haiku.generator.infrastructure;

import akka.haiku.generator.application.ImageGenerator;
import akka.haiku.storage.application.BlobStorage;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class GeminiImageGenerator implements ImageGenerator {

  private final BlobStorage blobStorage;

  public GeminiImageGenerator(BlobStorage blobStorage) {
    this.blobStorage = blobStorage;
  }


  @Override
  public String generateImage(String userInput, String haiku) {

    try {
      String projectId = "akka-dev-ai";
      String location = "us-central1";
      String prompt = "Create a image will be a good background for the following haiku: \n " + haiku +
        "\n generated based on this input: \n" + userInput;

      final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
      PredictionServiceSettings predictionServiceSettings =
        PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();

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

        if (!predictResponse.getPredictionsList().isEmpty()) {

          Value prediction = predictResponse.getPredictionsList().get(0);
          Map<String, Value> fieldsMap = prediction.getStructValue().getFieldsMap();
          if (fieldsMap.containsKey("bytesBase64Encoded")) {
            String bytesBase64Encoded = fieldsMap.get("bytesBase64Encoded").getStringValue();
            byte[] imageBytes = Base64.getDecoder().decode(bytesBase64Encoded);

            return blobStorage.uploadPng(imageBytes, "generated-images", "image-");
          } else {
            throw new RuntimeException("No image data found in the prediction response.");
          }
        } else {
          throw new RuntimeException("No predictions returned from the model.");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private Value mapToValue(Map<String, Object> map) throws InvalidProtocolBufferException {
    Gson gson = new Gson();
    String json = gson.toJson(map);
    Value.Builder builder = Value.newBuilder();
    JsonFormat.parser().merge(json, builder);
    return builder.build();
  }
}
