package ca.keal.varianttap;

import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

/**
 * A superclass for an Activity with background music.
 */
public abstract class MusicActivity extends AppCompatActivity
    implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
      SharedPreferences.OnSharedPreferenceChangeListener {
  
  private static final String TAG = "MusicActivity";
  
  protected MediaPlayer music;
  
  private boolean soundOn;
  
  // For starting the music after both prepared and resumed
  private boolean isPrepared;
  private boolean isResumed;
  
  protected abstract AssetFileDescriptor getBackgroundMusicFD() throws IOException;
  
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    
    // Setup the audio resources
    music = new MediaPlayer();
    
    try {
      AssetFileDescriptor afd = getBackgroundMusicFD();
      music.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
    } catch (IOException e) {
      Log.e(TAG, "Loading of " + getClass().getName() + "'s music failed!", e);
      return;
    }
    
    music.setLooping(true);
    
    // Get soundOn from SharedPreferences, register this as a listener
    SharedPreferences prefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    soundOn = prefs.getBoolean(Util.PREF_SOUND_ON, getResources()
        .getBoolean(R.bool.default_sound_on));
    prefs.registerOnSharedPreferenceChangeListener(this);
    adjustVolumeForSoundOn();
    
    // Set the audio attributes/stream type, depending on API level
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      music.setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build());
    } else {
      //noinspection deprecation
      music.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }
    
    music.setOnPreparedListener(this);
    music.setOnErrorListener(this);
    
    isPrepared = false;
    isResumed = false;
    
    music.prepareAsync();
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    
    getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE)
        .unregisterOnSharedPreferenceChangeListener(this);
    
    music.stop();
    music.release();
    music = null;
  }
  
  @Override
  public void onPrepared(MediaPlayer mediaPlayer) {
    // Start the music after both the music is prepared and the activity is resumed
    isPrepared = true;
    if (isResumed) {
      music.start();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    // Start the music after both the music is prepared and the activity is resumed
    isResumed = true;
    if (isPrepared) {
      music.start();
    }
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    music.pause();
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (!key.equals(Util.PREF_SOUND_ON)) return;
    
    boolean newSoundOn = prefs.getBoolean(key, soundOn);
    if (newSoundOn == soundOn) return;
    
    soundOn = newSoundOn;
    adjustVolumeForSoundOn();
  }
  
  private void adjustVolumeForSoundOn() {
    float volume = soundOn ? 1f : 0f;
    music.setVolume(volume, volume);
  }
  
  @Override
  public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
    Log.e(TAG, "Error playing music: code = " + what + ", extra code = " + extra);
    return false;
  }
  
}