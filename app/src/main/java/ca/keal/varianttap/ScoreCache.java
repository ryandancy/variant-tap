package ca.keal.varianttap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import se.simbio.encryption.Encryption;

/**
 * Represents an encrypted cache of scores stored in internal storage, used when the user is not
 * signed in to GPGS.
 * 
 * GPGS will cache scores automatically if the user is signed in, but this does not work if the user
 * is not signed in. However, scores submitted when the user is not signed in should not be lost;
 * instead, this class is used to cache them and submit them later. The scores are encrypted in
 * order to prevent users on rooted devices from messing with the public leaderboards. Everything is
 * done async.
 */
class ScoreCache {
  
  private static final String TAG = "ScoreCache";
  
  // DO NOT CHANGE
  private static final String SCORE_CACHE_FILE = "score_cache";
  private static final String SCORE_CACHE_KEY = "VariantTapScoreCacheKey";
  private static final String SCORE_CACHE_SALT = "VariantTapScoreCacheIsSalty";
  private static final byte[] SCORE_CACHE_IV = {
      // probably horribly insecure, but what do I care
      25, 78, -42, -117, -52, 73, -58, 28, 31, -113, 64, 10, 23, -38, 106, -118
  };
  
  private Encryption encryption;
  
  public ScoreCache() {
    encryption = Encryption.getDefault(SCORE_CACHE_KEY, SCORE_CACHE_SALT, SCORE_CACHE_IV);
  }
  
  public void cache(Context context, Score... scores) {
    new CacheAsyncTask(context).execute(scores);
  }
  
  private class CacheAsyncTask extends AsyncTask<Score, Void, Void> {
    
    private Context context;
    
    private CacheAsyncTask(Context context) {
      this.context = context;
    }
    
    @Override
    protected Void doInBackground(Score... scores) {
      for (Score score : scores) {
        cacheSync(context, score);
      }
      return null;
    }
    
  }
  
  private void cacheSync(Context context, Score score) {
    Log.d(TAG, "Caching " + score + "...");
    
    // Encrypt it
    String storable = score.toStorableString();
    String encrypted;
    try {
      encrypted = encryption.encrypt(storable);
      Log.d(TAG, "Score encrypted to be cached");
    } catch (Exception e) {
      Log.e(TAG, "Error while encrypting cached score", e);
      return;
    }
    
    // Write it
    try {
      FileOutputStream fos = context.openFileOutput(SCORE_CACHE_FILE, Context.MODE_APPEND);
      fos.write((encrypted + "\n").getBytes());
      fos.close();
      Log.d(TAG, "Score cached successfully");
    } catch (FileNotFoundException e) {
      Log.wtf(TAG, "openFileOutput() should create the file!", e);
    } catch (IOException e) {
      Log.e(TAG, "Exception while caching score", e);
    }
  }
  
  /**
   * It is the caller's responsibility to ensure that the device is connected to the internet prior
   * to using this method.
   */
  public void submitCache(Context context, GoogleApiClient client) {
    new SubmitCacheAsyncTask(context).execute(client);
  }
  
  private class SubmitCacheAsyncTask extends AsyncTask<GoogleApiClient, Void, Void> {
    
    private Context context;
    
    private SubmitCacheAsyncTask(Context context) {
      this.context = context;
    }
    
    @Override
    protected Void doInBackground(GoogleApiClient... params) {
      submitCacheSync(context, params[0]);
      return null;
    }
    
  }
  
  private void submitCacheSync(Context context, GoogleApiClient client) {
    Log.d(TAG, "Submitting cached scores...");
    String cache;
    
    try {
      // Get the cache contents
      FileInputStream fis = context.openFileInput(SCORE_CACHE_FILE);
      StringBuilder cacheBuilder = new StringBuilder();
      byte[] buffer = new byte[1024];
      int n;
      
      while ((n = fis.read(buffer)) != -1) {
        cacheBuilder.append(new String(buffer, 0, n));
      }
      
      fis.close();
      cache = cacheBuilder.toString();
      
      // Clear the cache file
      context.deleteFile(SCORE_CACHE_FILE); // will be regenerated on next cache
    } catch (FileNotFoundException e) {
      // Nothing cached yet; do nothing
      Log.d(TAG, "No cached scores found to be submitted");
      return;
    } catch (IOException e) {
      Log.e(TAG, "Error while reading cache file", e);
      return;
    }
    
    // Get the individual scores, decrypt them
    String[] scoreStrs = cache.split("\n");
    Score[] scores = new Score[scoreStrs.length];
    
    for (int i = 0; i < scoreStrs.length; i++) {
      String scoreStr;
      try {
        scoreStr = encryption.decrypt(scoreStrs[i]);
      } catch (Exception e) {
        Log.e(TAG, "Exception while decrypting score", e);
        return;
      }
      
      scores[i] = Score.fromStorableString(scoreStr);
    }
    
    // Submit the scores
    for (Score score : scores) {
      score.submit(client);
    }
    
    Log.d(TAG, "Decrypted and submitted " + scores.length + " scores");
  }
  
}