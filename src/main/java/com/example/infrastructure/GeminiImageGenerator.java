package com.example.infrastructure;

import com.example.application.ImageGenerator;

public class GeminiImageGenerator implements ImageGenerator {

  @Override
  public String generateImage(String prompt) {
    return "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Manta_birostris-Thailand.jpg/640px-Manta_birostris-Thailand.jpg";
  }
}
