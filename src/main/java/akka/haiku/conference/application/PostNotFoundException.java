package akka.haiku.conference.application;

import akka.javasdk.CommandException;

public class PostNotFoundException extends CommandException {
  public PostNotFoundException(String s) {
    super(s);
  }
}
