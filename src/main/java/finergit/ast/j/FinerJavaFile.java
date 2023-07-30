package finergit.ast.j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import finergit.FinerGitConfig;
import finergit.ast.FinerModule;

public class FinerJavaFile extends FinerJavaModule {

  private static final String FILE_EXTENSION = ".pjava";

  public final Path directory;
  private final List<FinerModule> innerJavaModules;

  public FinerJavaFile(final Path directory, final String name, final FinerGitConfig config) {
    super(name, null, config);
    this.directory = directory;
    this.innerJavaModules = new ArrayList<>();
  }

  public void addFinerJavaModule(final FinerModule module) {
    this.innerJavaModules.add(module);
  }

  public List<FinerModule> getInnerJavaModules() {
    return this.innerJavaModules;
  }

  @Override
  public Path getDirectory() {
    return this.directory;
  }

  @Override
  public String getExtension() {
    return FILE_EXTENSION;
  }

  /**
   * ベースネーム（拡張子がないファイル名）を返す．
   *
   * @return
   */
  @Override
  public String getBaseName() {
    return this.name;
  }
}
