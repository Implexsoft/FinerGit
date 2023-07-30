package finergit.ast.kt;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;

import java.util.List;
import java.util.stream.Collectors;

public abstract class FinerKotlinModule extends FinerModule {
    public FinerKotlinModule(String name, FinerModule outerModule, FinerGitConfig config) {
        super(name, outerModule, config);
    }

    public List<String> getLines() {
        final boolean isMethodTokenIncluded = this.config.isMethodTokenIncluded();
        final boolean isTokenTypeIncluded = this.config.isTokenTypeIncluded();
        return this.getTokens().stream()
                .map(t -> t.toLine(isTokenTypeIncluded))
                .collect(Collectors.toList());
    }
}
