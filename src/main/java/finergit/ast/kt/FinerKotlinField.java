package finergit.ast.kt;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;
import finergit.ast.j.FinerJavaModule;

import java.nio.file.Path;

public class FinerKotlinField extends FinerKotlinModule {

  private static final String FIELD_EXTENSION = ".fkt";
  private static final String FIELD_DELIMITER = "#";

  public FinerKotlinField(final String name, final FinerModule outerModule,
                          final FinerGitConfig config) {
    super(name, outerModule, config);
  }

  @Override
  public Path getDirectory() {
    return this.outerModule.getDirectory();
  }

  @Override
  public String getExtension() {
    return FIELD_EXTENSION;
  }

  /**
   * ベースネーム（拡張子がないファイル名）を返す．
   *
   * @return
   */
  @Override
  public String getBaseName() {
    return this.outerModule.getBaseName() + FIELD_DELIMITER + this.name;
  }
}
