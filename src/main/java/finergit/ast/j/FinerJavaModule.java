package finergit.ast.j;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import finergit.ast.j.token.*;
public abstract class FinerJavaModule extends FinerModule {
    public FinerJavaModule(String name, FinerModule outerModule, FinerGitConfig config) {
        super(name, outerModule, config);
    }

    public List<String> getLines() {
        final boolean isMethodTokenIncluded = this.config.isMethodTokenIncluded();
        final boolean isTokenTypeIncluded = this.config.isTokenTypeIncluded();
        return this.getTokens().stream()
                .filter(t -> isMethodTokenIncluded || LEFTMETHODPAREN.class != t.getClass())
                .filter(t -> isMethodTokenIncluded || RIGHTMETHODPAREN.class != t.getClass())
                .filter(t -> isMethodTokenIncluded || LEFTMETHODBRACKET.class != t.getClass())
                .filter(t -> isMethodTokenIncluded || RIGHTMETHODBRACKET.class != t.getClass())
                .filter(t -> isMethodTokenIncluded || METHODDECLARATIONSEMICOLON.class != t.getClass())
                .map(t -> t.toLine(isTokenTypeIncluded))
                .collect(Collectors.toList());
    }
}
