package finergit.ast.j.token;

import finergit.ast.j.FinerJavaField;

public class FinerJavaFieldToken extends JavaToken {

  final FinerJavaField finerJavaField;

  public FinerJavaFieldToken(final String value, final FinerJavaField finerJavaField) {
    super(value);
    this.finerJavaField = finerJavaField;
  }
}
