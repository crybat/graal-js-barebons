package pintomau.js;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Result {
  private final String map1;
  public final String map2;
  public final double max;

  @JsonCreator
  public Result(
      @JsonProperty("map1") final String map1,
      @JsonProperty("map2") final String map2,
      @JsonProperty("max") final double max) {
    this.map1 = map1;
    this.map2 = map2;
    this.max = max;
  }

  public String getMap1() {
    return map1;
  }

  public String getMap2() {
    return map2;
  }

  public double getMax() {
    return max;
  }

  @Override
  public String toString() {
    return "Result{" + "map1='" + map1 + '\'' + ", map2='" + map2 + '\'' + ", max=" + max + '}';
  }
}
