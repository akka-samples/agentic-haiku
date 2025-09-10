package akka.haiku;

import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.haiku.infrastructure.GeminiImageGenerator;
import com.typesafe.config.Config;

@Setup
public class Bootstrap implements ServiceSetup {

  public Bootstrap(Config config) {

  }

  @Override
  public DependencyProvider createDependencyProvider() {
    return DependencyProvider.single(new GeminiImageGenerator());
  }
}
