package finergit.ast;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import finergit.FinerGitConfig;
import finergit.ast.j.token.JavaToken;
import finergit.ast.j.token.LEFTMETHODBRACKET;
import finergit.ast.j.token.LEFTMETHODPAREN;
import finergit.ast.j.token.METHODDECLARATIONSEMICOLON;
import finergit.ast.j.token.RIGHTMETHODBRACKET;
import finergit.ast.j.token.RIGHTMETHODPAREN;

public abstract class FinerModule {

  private static final Logger log = LoggerFactory.getLogger(FinerModule.class);

  public final String name;
  public final FinerModule outerModule;
  protected final FinerGitConfig config;
  private final List<Token> tokens;

  public FinerModule(final String name, final FinerModule outerModule,
              final FinerGitConfig config) {
    this.name = name;
    this.outerModule = outerModule;
    this.config = config;
    this.tokens = new ArrayList<>();
  }

  public boolean addToken(final Token token) {
    return this.tokens.add(token);
  }

  public void clearTokens() {
    this.tokens.clear();
  }

  public List<Token> getTokens() {
    return this.tokens;
  }

  public abstract List<String> getLines();

  public abstract Path getDirectory();

  /**
   * このモジュールのファイル名を返す．モジュールのファイル名は，"外側のモジュール名 + 自分のベースネーム + 拡張子"である．
   * モジュール名がしきい値よりも長い場合には，しきい値の長さになるように縮められる．なお，その場合はモジュール名から算出したハッシュ値が後ろに付く．
   *
   * @return
   */
  public final String getFileName() {
    String name = this.getBaseName() + this.getExtension();
    final int maxFileNameLength = this.config.getMaxFileNameLength();
    if (maxFileNameLength < name.length()) {
      //log.info("\"{}\" is shrinked to {} characters due to too long name", name, maxFileNameLength);
      name = this.shrink(name);
    }
    return name;
  }

  private String shrink(final String name) {
    final int maxFileNameLength = this.config.getMaxFileNameLength();
    final int hashLength = this.config.getHashLength();
    final String sha1 = DigestUtils.sha1Hex(name)
        .substring(0, hashLength);
    return name.substring(0, maxFileNameLength - (hashLength + getExtension().length() + 1))
        + "_"
        + sha1
        + getExtension();
  }

  public final Path getPath() {
    return this.getDirectory()
        .resolve(this.getFileName());
  }

  /**
   * 拡張子を返す．
   *
   * @return
   */
  public abstract String getExtension();

  /**
   * ベースネーム（拡張子がないファイル名）を返す．
   *
   * @return
   */
  abstract public String getBaseName();
}
