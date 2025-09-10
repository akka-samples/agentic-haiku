package akka.haiku.domain;


import akka.haiku.domain.TextInputCollectorEvent.TextInputAdded;


import java.util.ArrayList;
import java.util.List;

public record TextInputCollector(String collectorId, int limit, List<String> inputs) {

  private TextInputCollector add(String input) {
    if (inputs.size() == limit) {
      ArrayList<String> newList = new ArrayList<>();
      newList.add(input);
      return new TextInputCollector(collectorId, limit, newList);
    } else {
      inputs.add(input);
      return this;
    }
  }



  public TextInputCollector apply(TextInputCollectorEvent event) {
    return switch (event) {
      case TextInputAdded textInputAdded -> add(textInputAdded.input()); //TODO add limit
    };
  }

  @Override
  public List<String> inputs() {
    return List.copyOf(inputs);
  }
}
