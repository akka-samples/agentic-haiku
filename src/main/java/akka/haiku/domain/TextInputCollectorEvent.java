package akka.haiku.domain;

public sealed interface TextInputCollectorEvent {

  record TextInputAdded(String collectorId, String inputId, String input) implements TextInputCollectorEvent {}
}
