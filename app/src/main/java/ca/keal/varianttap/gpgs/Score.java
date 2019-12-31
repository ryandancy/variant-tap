package ca.keal.varianttap.gpgs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;

/**
 * Represents a score, with a leaderboard.
 */
// TODO maybe add scoretags?
public class Score {
  
  private static final String TAG = "Score";
  
  @NonNull
  private final String leaderboardId;
  private final int score;
  
  public Score(@NonNull String leaderboardId, int score) {
    this.leaderboardId = leaderboardId;
    this.score = score;
  }
  
  public Score(Context context, @StringRes int leaderboardResId, int score) {
    this(context.getString(leaderboardResId), score);
  }
  
  @NonNull
  public String getLeaderboardId() {
    return leaderboardId;
  }
  
  public int getScore() {
    return score;
  }
  
  /** Submit this Score to the client, assuming that the client is signed in. */
  public void submit(Context context, GoogleSignInAccount account) {
    Games.getLeaderboardsClient(context, account).submitScore(leaderboardId, score);
  }
  
  public String toStorableString() {
    return leaderboardId + " " + score;
  }
  
  public static Score fromStorableString(String string) {
    String[] parts = string.split(" ");
    if (parts.length != 2) {
      Log.w(TAG, "Could not recover Score from storable string \"" + string + "\"");
      return null;
    }
    
    String leaderboardId = parts[0];
    int score = Integer.parseInt(parts[1]);
    
    return new Score(leaderboardId, score);
  }
  
  @Override
  public String toString() {
    return "Score{leaderboardId=" + leaderboardId + ", score=" + score + "}";
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Score)) return false;
    Score other = (Score) obj;
    return leaderboardId.equals(other.leaderboardId) && score == other.score;
  }
  
  @Override
  public int hashCode() {
    return 43 * leaderboardId.hashCode() * score;
  }
  
}