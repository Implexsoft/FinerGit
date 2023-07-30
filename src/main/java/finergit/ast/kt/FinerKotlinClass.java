package finergit.ast.kt;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FinerKotlinClass extends FinerModule {

  private static final String CLASS_EXTENSION = ".ckt";
  private static final String CLASS_DELIMITER = "$";

  public FinerKotlinClass(final String name, final FinerModule outerModule,
                          final FinerGitConfig config) {
    super(name, outerModule, config);
  }

  @Override
  public List<String> getLines() {
    final boolean isMethodTokenIncluded = this.config.isMethodTokenIncluded();
    final boolean isTokenTypeIncluded = this.config.isTokenTypeIncluded();
    return this.getTokens().stream()
            .map(t -> t.toLine(isTokenTypeIncluded))
            .collect(Collectors.toList());
  }

  @Override
  public Path getDirectory() {
    return this.outerModule.getDirectory();
  }

  @Override
  public String getExtension() {
    return CLASS_EXTENSION;
  }

  /**
   * ベースネーム（拡張子がないファイル名）を返す．
   *
   * @return
   */
  @Override
  public String getBaseName() {

    final StringBuilder builder = new StringBuilder();

    // 外側のモジュールがファイル名の場合は，
    // ファイル名とこのクラス名が違う場合のみ，
    // ファイル名を含める
    if (FinerKotlinFile.class == this.outerModule.getClass()) {
      if (!this.name.equals(this.outerModule.name)) {
        builder.append("[")
            .append(this.outerModule.name)
            .append("]");
      }
    }

    // 内部クラスの場合は外側のクラス名を含める
    else if (FinerKotlinClass.class == this.outerModule.getClass()) {
      builder.append(this.outerModule.getBaseName())
          .append(CLASS_DELIMITER);
    }

    builder.append(this.name);
    return builder.toString();
  }
}
