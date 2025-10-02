package akka.haiku.generator.infrastructure;

import akka.haiku.generator.application.ImageGenerator;
import akka.haiku.storage.application.BlobStorage;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.*;


public class GeminiImageGenerator implements ImageGenerator {

  private final BlobStorage blobStorage;

  private final Logger logger = getLogger(GeminiImageGenerator.class);

  public GeminiImageGenerator(BlobStorage blobStorage) {
    this.blobStorage = blobStorage;
  }


  @Override
  public String generateImage(String userInput, String haiku) {

    try {
      String projectId = "akka-dev-ai";
      String location = "us-central1";
      String prompt = "Create a image inspired by this Haiku (between [])." +
                "[" + haiku + "]" +
                "The image should be in the style of a Japanese painting using light tones." +
                "If text is included in the image, it must strictly be part of the haiku. " +
                "No other text should be included" +
                "\n The Haiku itself was generated based on this input: \n" + userInput;


      String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
      PredictionServiceSettings predictionServiceSettings =
        PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();

      // Initialize client that will be used to send requests. This client only needs to be created
      // once, and can be reused for multiple requests.
      try (PredictionServiceClient predictionServiceClient =
               PredictionServiceClient.create(predictionServiceSettings)) {

        EndpointName endpointName =
          EndpointName.ofProjectLocationPublisherModelName(
            projectId, location, "google", "imagen-4.0-generate-001");

        Map<String, Object> instancesMap = new HashMap<>();
        instancesMap.put("prompt", prompt);
        Value instances = mapToValue(instancesMap);

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("sampleCount", 1);
        // You can't use a seed value and watermark at the same time.
        // paramsMap.put("seed", 100);
        // paramsMap.put("addWatermark", false);
        paramsMap.put("aspectRatio", "1:1");
        paramsMap.put("sampleImageSize", "1k");
        paramsMap.put("safetyFilterLevel", "block_some");
        paramsMap.put("personGeneration", "allow_adult");
        paramsMap.put("mimeType", "image/jpeg");
        paramsMap.put("compressionQuality", "60");
        paramsMap.put("outputOptions.mimeType", "image/jpeg"); // Request JPEG output from the model
        paramsMap.put("outputOptions.compressionQuality", "60");
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

            // Compress if over 900KB (downscale PNG, do not convert to JPEG)
            final int MAX_SIZE = 900 * 1024;
            byte[] uploadBytes = imageBytes;
            if (uploadBytes.length > MAX_SIZE) {
              logger.debug("generated file is larger than 900kb, downscaling it as PNG...");
              java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
              int width = img.getWidth();
              int height = img.getHeight();
              boolean fits = false;
              while (uploadBytes.length > MAX_SIZE && width > 32 && height > 32) {
                width = (int) (width * 0.9); // Reduce by 10%
                height = (int) (height * 0.9);
                java.awt.image.BufferedImage scaledImg = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = scaledImg.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(img, 0, 0, width, height, null);
                g2d.dispose();
                java.io.ByteArrayOutputStream pngOut = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(scaledImg, "png", pngOut);
                uploadBytes = pngOut.toByteArray();
                img = scaledImg;
              }
              if (uploadBytes.length > MAX_SIZE) {
                logger.warn("Warning: Could not downscale PNG below 900KB. Uploading best effort.");
              }
            }
            // Upload as PNG
            return blobStorage.uploadPng(uploadBytes, "generated-images", "image-");
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
