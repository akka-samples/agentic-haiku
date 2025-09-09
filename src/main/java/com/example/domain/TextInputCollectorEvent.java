package com.example.domain;

import java.util.List;

public sealed interface TextInputCollectorEvent {

  record TextInputAdded(String input) implements TextInputCollectorEvent {}
  record AllInputsCollected(String collectorId, String collectionId, List<String> inputs) implements TextInputCollectorEvent {}
}
