package ca.keal.varianttap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import static android.content.Context.MODE_PRIVATE;

public class SoundToggleCircleButton extends BaseCircleButton
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  
  private boolean soundOn;
  private boolean registered = false; // is the OnSharedPreferenceChangeListener registered?
  
  public SoundToggleCircleButton(Context context) {
    this(context, null);
  }
  
  public SoundToggleCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.toggleSoundCircleButtonStyle);
  }
  
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    
    SharedPreferences prefs = context.getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    soundOn = prefs.getBoolean(Util.PREF_SOUND_ON, true); // sound on by default
    
    if (soundOn) {
      toggleOn();
    } else {
      toggleOff();
    }
    
    if (!registered) {
      prefs.registerOnSharedPreferenceChangeListener(this);
      registered = true;
    }
  }
  
  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (registered) {
      context.getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE)
          .unregisterOnSharedPreferenceChangeListener(this);
      registered = false;
    }
  }
  
  @Override
  protected void onClick() {
    soundOn = !soundOn;
    
    if (soundOn) {
      toggleOn();
    } else {
      toggleOff();
    }
    
    // Write the sound to the SharedPreferences
    SharedPreferences.Editor editPrefs = context.getSharedPreferences(
        Util.PREF_FILE, Context.MODE_PRIVATE).edit();
    editPrefs.putBoolean(Util.PREF_SOUND_ON, soundOn);
    editPrefs.apply();
  }
  
  private void toggleOn() {
    int circleButtonColor = ContextCompat.getColor(context, R.color.circleButtonColor);
    getDrawable().setColorFilter(circleButtonColor, PorterDuff.Mode.MULTIPLY);
    setBackgroundResource(R.drawable.circle);
    setContentDescription(context.getString(R.string.sound_on_desc));
  }
  
  private void toggleOff() {
    int disabledColor = ContextCompat.getColor(context, R.color.circleButtonDisabled);
    getDrawable().setColorFilter(disabledColor, PorterDuff.Mode.MULTIPLY);
    setBackgroundResource(R.drawable.circle_disabled);
    setContentDescription(context.getString(R.string.sound_off_desc));
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (!Util.PREF_SOUND_ON.equals(key)) return;
    soundOn = prefs.getBoolean(key, false);
    if (soundOn) {
      toggleOn();
    } else {
      toggleOff();
    }
  }
  
}