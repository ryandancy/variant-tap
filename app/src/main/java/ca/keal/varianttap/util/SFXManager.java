package ca.keal.varianttap.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import androidx.annotation.RawRes;
import android.util.SparseIntArray;

import ca.keal.varianttap.R;

public class SFXManager implements SharedPreferences.OnSharedPreferenceChangeListener {
  
  /** All SFX known to the SFXManager. Note that more may be loaded at runtime. */
  private static int[] ALL_SFX = {R.raw.success, R.raw.lose};
  
  private SoundPool sound;
  private SparseIntArray resToSoundIds = new SparseIntArray();
  private SparseIntArray resToStreamIds = new SparseIntArray();
  
  private boolean soundOn;
  
  public SFXManager(Context context) {
    // Initialize the SoundPool using AudioAttributes if in Lollipop or above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      AudioAttributes attrs = new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build();
      sound = new SoundPool.Builder()
          .setMaxStreams(10)
          .setAudioAttributes(attrs)
          .build();
    } else {
      sound = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
    }
    
    // Load the SFX, put them in the map
    for (int resId : ALL_SFX) {
      int soundId = sound.load(context, resId, 0);
      resToSoundIds.put(resId, soundId);
    }
    
    // Get soundOn from SharedPreferences
    SharedPreferences prefs = context.getSharedPreferences(Util.PREF_FILE, Context.MODE_PRIVATE);
    soundOn = prefs.getBoolean(Util.PREF_SOUND_ON, context.getResources()
        .getBoolean(R.bool.default_sound_on));
    prefs.registerOnSharedPreferenceChangeListener(this);
  }
  
  public void play(Context context, @RawRes int resId) {
    if (!soundOn) return;
    
    int soundId = resToSoundIds.get(resId, -1);
    
    if (soundId == -1) {
      // resId isn't in the SoundPool; put it there
      soundId = sound.load(context, resId, 0);
      resToSoundIds.put(resId, soundId);
    }
    
    int streamId = sound.play(soundId, 1f, 1f, 0, 0, 1f);
    resToStreamIds.put(resId, streamId);
  }
  
  public void release(Context context) {
    context.getSharedPreferences(Util.PREF_FILE, Context.MODE_PRIVATE)
        .unregisterOnSharedPreferenceChangeListener(this);
    
    sound.release();
    resToSoundIds.clear();
    resToStreamIds.clear();
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (!key.equals(Util.PREF_SOUND_ON)) return;
    soundOn = prefs.getBoolean(key, soundOn);
  }
  
}