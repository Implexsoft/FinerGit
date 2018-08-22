package jp.kusumotolab.finergit.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

public class RevCommitUtil {

  // 引数で与えられた RevCommit のハッシュの最初の7文字を返す
  public static String getAbbreviatedID(final AnyObjectId anyObjectId) {
    return anyObjectId.abbreviate(7)
        .name();
  }

  // 引数で与えられた RevCommit の Author Name を返す
  public static String getAuthor(final RevCommit commit) {
    return commit.getAuthorIdent()
        .getName();
  }

  // 引数で与えられた RevCommit の時刻情報を返す
  public static String getDate(final RevCommit commit) {
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
    final PersonIdent authorIdent = commit.getAuthorIdent();
    final Date date = authorIdent.getWhen();
    return simpleDateFormat.format(date);
  }

  public static boolean isBugFix(final RevCommit commit) {
    final String message = commit.getFullMessage();
    return message.contains(" bug") || //
        message.contains(" fix") || //
        message.contains("バグ") || //
        message.contains("修正");
  }
}