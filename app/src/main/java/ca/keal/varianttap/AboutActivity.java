package ca.keal.varianttap;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
  
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);
  }
  
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
  }
  
}