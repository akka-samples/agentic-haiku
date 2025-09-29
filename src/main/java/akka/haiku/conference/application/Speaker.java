package akka.haiku.conference.application;

public record Speaker(int id,
                      String firstName,
                      String lastName,
                      String fullName,
                      String twitterHandle,
                      String blueskyUsername) {
}
