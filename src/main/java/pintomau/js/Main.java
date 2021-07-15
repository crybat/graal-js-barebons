package pintomau.js;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import org.graalvm.polyglot.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

  public static final int WARMUP = 100;
  public static final int ITERATIONS = 1000;

  public static void main(final String[] args) throws IOException, InterruptedException {
    benchJslt();
    benchGraalPolyglot();
    benchGraalPolyglotJavaType();
    benchGraalMultiThread();
    testES2016Imports();
  }

  private static final String JSLT_ARRAY =
      "[ for (.)\n"
          + "  {\n"
          + "    \"map1\": .el1,\n"
          + "    \"map2\": .el2,\n"
          + "    \"max\": max(.i, 0)\n"
          + "  }\n"
          + "]\n";

  /** Basic JSLT mapping to compare the JS alternatives to */
  private static void benchJslt() {
    final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    final List<Object> originals =
        IntStream.range(0, 100)
            .mapToObj(i -> new Original("el1", "el2", ThreadLocalRandom.current().nextInt()))
            .collect(Collectors.toList());

    final JsonNode toConvert = OBJECT_MAPPER.valueToTree(originals);
    final Expression arrayMapper = Parser.compileString(JSLT_ARRAY);

    System.out.println("warming up...");
    for (int i = 0; i < WARMUP; i++) {
      final JsonNode result = arrayMapper.apply(toConvert);
      final List<Result> results = OBJECT_MAPPER.convertValue(result, new TypeReference<>() {});
    }
    System.out.println("warmup finished, now measuring...");

    long sum = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      final long start = System.currentTimeMillis();
      final JsonNode result = arrayMapper.apply(toConvert);
      final List<Result> results = OBJECT_MAPPER.convertValue(result, new TypeReference<>() {});
      sum += System.currentTimeMillis() - start;
      //      System.out.println(results);
    }
    System.out.println(String.format("JSLT: %s ms", sum));
  }

  /**
   * An implementation NOT using java types within the JS scripts.
   *
   * <p>Tends to be slower than using java types since it needs to convert the intermediate JSON
   * type to an object using jackson
   *
   * @throws IOException
   */
  private static void benchGraalPolyglot() throws IOException {
    final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    final Context CTX =
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowExperimentalOptions(true)
            // needed to allow processing Lists from within JS as if they were JS arrays
            .option("js.experimental-foreign-object-prototype", "true")
            .option("js.strict", "true")
            .build();

    final File file = new File(Main.class.getClassLoader().getResource("mapper.js").getFile());
    CTX.eval(Source.newBuilder("js", file).build());
    //    final Value mapper = CTX.getBindings("js").getMember("map");
    final Value arrayMapper = CTX.getBindings("js").getMember("mapArray");

    final List<Object> originals =
        IntStream.range(0, 100)
            .mapToObj(i -> new Original("el1", "el2", ThreadLocalRandom.current().nextInt()))
            .collect(Collectors.toList());

    System.out.println("warming up...");
    for (int i = 0; i < WARMUP; i++) {
      final Value result = arrayMapper.execute(originals);
      final List<Result> results =
          OBJECT_MAPPER.convertValue(result.as(List.class), new TypeReference<>() {});
    }
    System.out.println("warmup finished, now measuring...");

    long sum = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      final long start = System.currentTimeMillis();
      final Value result = arrayMapper.execute(originals);
      final List<Result> results =
          OBJECT_MAPPER.convertValue(result.as(List.class), new TypeReference<>() {});
      sum += System.currentTimeMillis() - start;
      //            System.out.println(results);
    }
    System.out.println(String.format("GraalJS: %s ms", sum));
  }

  /**
   * An implementation USING java types from within the JS scripts.
   *
   * <p>Tends to be faster than using Jackson to interpret an intermediate JSON object returned from
   * the JS scripts.
   *
   * @throws IOException
   */
  private static void benchGraalPolyglotJavaType() throws IOException {
    final Context CTX =
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(className -> true)
            .allowExperimentalOptions(true)
            // needed to allow processing Lists from within JS as if they were JS arrays
            .option("js.experimental-foreign-object-prototype", "true")
            .option("js.strict", "true")
            .build();

    final File file =
        new File(Main.class.getClassLoader().getResource("mapper-to-java.js").getFile());
    CTX.eval(Source.newBuilder("js", file).build());
    //    final Value mapper = CTX.getBindings("js").getMember("map");
    final Value arrayMapper = CTX.getBindings("js").getMember("mapArray");

    final List<Object> originals =
        IntStream.range(0, 100)
            .mapToObj(i -> new Original("el1", "el2", ThreadLocalRandom.current().nextInt()))
            .collect(Collectors.toList());

    System.out.println("warming up...");
    for (int i = 0; i < WARMUP; i++) {
      final Value result = arrayMapper.execute(originals);
      final List<Result> results = result.as(new TypeLiteral<>() {});
    }
    System.out.println("warmup finished, now measuring...");

    long sum = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      final long start = System.currentTimeMillis();
      final Value result = arrayMapper.execute(originals);
      final List<Result> results = result.as(new TypeLiteral<>() {});
      sum += System.currentTimeMillis() - start;
      //            System.out.println(results);
    }
    System.out.println(String.format("GraalJS Java Types: %s ms", sum));
  }

  /**
   * An example showing how multi-threading and code caching works
   *
   * @throws IOException
   * @throws InterruptedException
   */
  private static void benchGraalMultiThread() throws IOException, InterruptedException {
    // https://www.graalvm.org/reference-manual/embed-languages/#code-caching-across-multiple-contexts
    // A context can only be executed by a single thread
    // We enable code caching to reuse compiled code, reducing memory usage and warm-up times.
    final File file =
        new File(Main.class.getClassLoader().getResource("mapper-to-java.js").getFile());
    final Source js = Source.newBuilder("js", file).build();

    // the engine instance dictates the caching scope
    final Engine engine = Engine.create();

    final List<Object> originals =
        IntStream.range(0, 100)
            .mapToObj(i -> new Original("el1", "el2", ThreadLocalRandom.current().nextInt()))
            .parallel()
            .collect(Collectors.toList());

    final int threads = 4;

    for (int i = 0; i < WARMUP; i++) {
      final CountDownLatch countDownLatch = new CountDownLatch(threads);
      for (int j = 0; j < threads; j++) {
        new Thread(
                () -> {
                  final Context CTX =
                      Context.newBuilder("js")
                          .engine(engine)
                          .allowHostAccess(HostAccess.ALL)
                          .allowHostClassLookup(className -> true)
                          .allowIO(true)
                          .allowExperimentalOptions(true)
                          .option("js.experimental-foreign-object-prototype", "true")
                          .option("js.strict", "true")
                          .build();

                  CTX.eval(js);
                  final Value arrayMapper = CTX.getBindings("js").getMember("mapArray");

                  final Value result = arrayMapper.execute(originals);
                  final List<Result> results = result.as(new TypeLiteral<>() {});
                  //              System.out.println(results);
                  countDownLatch.countDown();
                })
            .start();
      }
      countDownLatch.await(5, TimeUnit.SECONDS);
    }

    final AtomicLong sum = new AtomicLong(0L);
    final CountDownLatch countDownLatch = new CountDownLatch(threads);
    for (int j = 0; j < threads; j++) {
      new Thread(
              () -> {
                final long start = System.currentTimeMillis();
                final Context CTX =
                    Context.newBuilder("js")
                        .engine(engine)
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(className -> true)
                        .allowIO(true)
                        .allowExperimentalOptions(true)
                        .option("js.experimental-foreign-object-prototype", "true")
                        .option("js.strict", "true")
                        .build();

                CTX.eval(js);
                final Value arrayMapper = CTX.getBindings("js").getMember("mapArray");

                final Value result = arrayMapper.execute(originals);
                final List<Result> results = result.as(new TypeLiteral<>() {});
                //                System.out.println(results);
                sum.addAndGet(System.currentTimeMillis() - start);
                countDownLatch.countDown();
              })
          .start();
    }
    countDownLatch.await(5, TimeUnit.SECONDS);
    System.out.println(String.format("SUM MT %s ms", sum.get()));
  }

  /**
   * Example demonstrating ES6 modules
   *
   * @throws IOException
   */
  private static void testES2016Imports() throws IOException {
    final Context CTX =
        Context.newBuilder("js")
            // allows access to classes, methods, etc...
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(className -> true)
            // allows access to IO operations, enabling ES6 modules
            .allowIO(true)
            .allowExperimentalOptions(true)
            .option("js.experimental-foreign-object-prototype", "true")
            .option("js.strict", "true")
            .build();

    final File file = new File(Main.class.getClassLoader().getResource("Bar.mjs").getFile());
    final Source js = Source.newBuilder("js", file).build();
    CTX.eval(js);
  }
}
