package finergit.ast;

public abstract class Token {
    final public String value;
    public int line;
    public int index;

    public Token(final String value) {
        this.value = value;
        this.line = 0;
        this.index = 0;
    }

    public Token(final String value, final int line, final int index) {
        this.value = value;
        this.line = line;
        this.index = index;
    }
    final public String toLine(final boolean tokenTypeIncluded) {
        final StringBuilder text = new StringBuilder();
        text.append(this.value);
        if (tokenTypeIncluded) {
            text.append("\t");
            text.append(this.getClass()
                    .getSimpleName());
        }
        return text.toString();
    }
}
