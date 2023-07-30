package finergit.ast.kt;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;

import java.nio.file.Path;

public class FinerKotlinMethod extends FinerKotlinModule {

  private static final String METHOD_EXTENSION = ".mkt";
  private static final String METHOD_DELIMITER = "#";

  public FinerKotlinMethod(final String name, final FinerModule outerModule,
                           final FinerGitConfig config) {
    super(name, outerModule, config);
  }

  @Override
  public Path getDirectory() {
    return this.outerModule.getDirectory();
  }

  @Override
  public String getExtension() {
    return METHOD_EXTENSION;
  }

  /**
   * ベースネーム（拡張子がないファイル名）を返す．
   *
   * @return
   */
  @Override
  public String getBaseName() {
    return this.outerModule.getBaseName() + METHOD_DELIMITER + this.name;
  }
}
