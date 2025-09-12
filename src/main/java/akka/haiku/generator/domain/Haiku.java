package akka.haiku.generator.domain;

public record Haiku(String line1, String line2, String line3) {

  public String formatted() {
    return line1 + "\n" + line2 + "\n" +  line3;
  }
}
