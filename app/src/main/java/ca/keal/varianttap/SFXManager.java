package ca.keal.varianttap;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.annotation.RawRes;
import android.util.Log;
import android.util.SparseIntArray;

class SFXManager {
  
  /** All SFX known to the SFXManager. Note that more may be loaded at runtime. */
  private static int[] ALL_SFX = {R.raw.success, R.raw.lose};
  
  private SoundPool sound;
  private SparseIntArray resToSoundIds = new SparseIntArray();
  private SparseIntArray resToStreamIds = new SparseIntArray();
  
  SFXManager(Context context) {
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
      //noinspection deprecation
      sound = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
    }
    
    // Load the SFX, put them in the map
    for (int resId : ALL_SFX) {
      int soundId = sound.load(context, resId, 0);
      resToSoundIds.put(resId, soundId);
    }
  }
  
  void play(Context context, @RawRes int resId) {
    int soundId = resToSoundIds.get(resId, -1);
    
    if (soundId == -1) {
      // resId isn't in the SoundPool; put it there
      soundId = sound.load(context, resId, 0);
      resToSoundIds.put(resId, soundId);
    }
    
    int streamId = sound.play(soundId, 1f, 1f, 0, 0, 1f);
    resToStreamIds.put(resId, streamId);
  }
  
  void pause(@RawRes int resId) {
    int streamId = resToSoundIds.get(resId, -1);
    if (streamId == -1) {
      Log.e(getClass().getName(), "Attempted to pause nonexistent or non-playing SFX!");
      return;
    }
    
    sound.pause(streamId);
  }
  
  void resume(@RawRes int resId) {
    int streamId = resToSoundIds.get(resId, -1);
    if (streamId == -1) {
      Log.e(getClass().getName(), "Attempted to resume nonexistent or non-paused SFX!");
      return;
    }
    
    sound.resume(streamId);
  }
  
  void stop(@RawRes int resId) {
    int streamId = resToSoundIds.get(resId, -1);
    if (streamId == -1) {
      Log.e(getClass().getName(), "Attempted to stop nonexistent or non-paused SFX!");
      return;
    }
  
    sound.stop(streamId);
  }
  
}