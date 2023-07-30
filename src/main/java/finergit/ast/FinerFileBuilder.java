package finergit.ast;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import finergit.ast.j.JavaFileVisitor;
import finergit.ast.kt.KotlinFileVisitor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import finergit.FinerGitConfig;
import finergit.JavaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinerFileBuilder {
  private static final Logger logger = LoggerFactory.getLogger(FinerFileBuilder.class);

  private final FinerGitConfig config;

  public FinerFileBuilder(final FinerGitConfig config) {
    this.config = config;
  }

  public List<FinerModule> getFinerJavaModules(final String path, final String text) {
    final ASTParser parser = createNewParser();
    parser.setSource(text.toCharArray());
    final CompilationUnit ast = (CompilationUnit) parser.createAST(null);

    // 与えられたASTに問題があるときは何もしない
    final IProblem[] problems = ast.getProblems();
    if (null == problems || 0 < problems.length) {
      return Collections.emptyList();
    }

    final JavaFileVisitor visitor = new JavaFileVisitor(Paths.get(path), this.config);
    ast.accept(visitor);
    return visitor.getFinerJavaModules();
  }

  public List<FinerModule> getFinerKotlinModules(final String path, final String text) {

    KotlinFileVisitor visitor = new KotlinFileVisitor(Paths.get(path), this.config);
    try {
      return visitor.visit(path, text);
    } catch (IOException e) {
      logger.warn("Error while processing file: {}. Error: {}", path, e.getMessage());
      return Collections.emptyList();
    }
  }

  private ASTParser createNewParser() {
    ASTParser parser = ASTParser.newParser(AST.JLS15);
    final JavaVersion javaVersion = this.config.getJavaVersion();
    final Map<String, String> options = javaVersion.getOptions();
    parser.setCompilerOptions(options);

    // TODO: Bindingが必要か検討
    parser.setResolveBindings(false);
    parser.setBindingsRecovery(false);
    parser.setEnvironment(null, null, null, true);

    return parser;
  }
}
