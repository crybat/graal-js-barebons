package pintomau.js;

public final class Original {
  private final String el1;
  private final String el2;
  private final int i;

  public Original(final String el1, final String el2, final int i) {
    this.el1 = el1;
    this.el2 = el2;
    this.i = i;
  }

  public String getEl1() {
    return el1;
  }

  public String getEl2() {
    return el2;
  }

  public int getI() {
    return i;
  }

  @Override
  public String toString() {
    return "Original{" + "el1='" + el1 + '\'' + ", el2='" + el2 + '\'' + ", i=" + i + '}';
  }
}
