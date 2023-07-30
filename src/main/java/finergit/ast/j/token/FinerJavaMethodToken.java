package finergit.ast.j.token;

import finergit.ast.j.FinerJavaMethod;

public class FinerJavaMethodToken extends JavaToken {

  final FinerJavaMethod finerJavaMethod;

  public FinerJavaMethodToken(final String value, final FinerJavaMethod finerJavaMethod) {
    super(value);
    this.finerJavaMethod = finerJavaMethod;
  }
}
