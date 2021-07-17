package pintomau.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.net.URL;

public class Main {

  public static void main(final String[] args) throws IOException, InterruptedException {
    testES2016Imports();
  }

  /**
   * Example demonstrating ES6 modules
   *
   * @throws IOException
   */
  private static void testES2016Imports() throws IOException {
    final Context CTX =
        Context.newBuilder("js")
            .allowIO(true)
            .build();

    final URL resource = Main.class.getClassLoader().getResource("Bar.mjs");
    final Source js =
        Source.newBuilder("js", resource).build();
    CTX.eval(js);
  }
}
