package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

public class SoundToggleCircleButton extends BaseCircleButton
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  
  private boolean soundOn;
  private boolean registered = false; // is the OnSharedPreferenceChangeListener registered?
  
  private int realBaseColor;
  
  public SoundToggleCircleButton(Context context) {
    this(context, null);
  }
  
  public SoundToggleCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.toggleSoundCircleButtonStyle);
    realBaseColor = baseColor;
  }
  
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    
    SharedPreferences prefs = context.getSharedPreferences(Util.PREF_FILE, Context.MODE_PRIVATE);
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
      context.getSharedPreferences(Util.PREF_FILE, Context.MODE_PRIVATE)
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
    baseColor = realBaseColor;
    setBackgroundResource(R.drawable.circle);
    getBackground().setColorFilter(baseColor, PorterDuff.Mode.MULTIPLY);
    setContentDescription(context.getString(R.string.sound_on_desc));
    fixPaddingOnApi19();
  }
  
  private void toggleOff() {
    int disabledColor = ContextCompat.getColor(context, R.color.circleButtonDisabled);
    baseColor = disabledColor;
    setBackgroundResource(R.drawable.circle_slashed);
    getBackground().setColorFilter(disabledColor, PorterDuff.Mode.MULTIPLY);
    setContentDescription(context.getString(R.string.sound_off_desc));
    fixPaddingOnApi19();
  }
  
  private void fixPaddingOnApi19() {
    if (Build.VERSION.SDK_INT <= 19) {
      int padding = getResources().getDimensionPixelSize(R.dimen.circle_button_padding);
      setPadding(padding, padding, padding, padding);
    }
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