package finergit.ast.j.token;

import finergit.ast.Token;

public abstract class JavaToken extends Token {

  JavaToken(final String value) {
    super(value);
  }

  JavaToken(String value, int line, int index) {
    super(value, line, index);
  }
}
