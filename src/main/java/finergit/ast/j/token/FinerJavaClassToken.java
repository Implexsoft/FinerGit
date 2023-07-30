package finergit.ast.j.token;

import finergit.ast.j.FinerJavaClass;

public class FinerJavaClassToken extends JavaToken {

  final FinerJavaClass finerJavaClass;

  public FinerJavaClassToken(final String value, final FinerJavaClass finerJavaClass) {
    super(value);
    this.finerJavaClass = finerJavaClass;
  }
}
