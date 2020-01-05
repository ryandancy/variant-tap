package ca.keal.varianttap.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.util.ReverseInterpolator;
import ca.keal.varianttap.util.Util;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * A {@link Fragment} which contains a button that shows the difficulty buttons in order to choose
 * the difficulty. Activities that contain this fragment may implement the
 * {@link DifficultyButtonsFragment.OnFragmentInteractionListener} interface to handle interaction
 * events.
 */
// Note: if you need to create an instance of this class for whatever reason, newInstance methods are in the Git log
public class DifficultyButtonsFragment extends Fragment implements View.OnClickListener,
    GPGSHelperClient {
  
  // The fragment initialization parameters
  private static final String ARG_SHOW_TEXT = "SHOW_TEXT";
  private static final String ARG_HIDE_TEXT = "HIDE_TEXT";
  
  // Fragment parameters
  private String showText = null;
  private String hideText = null;
  
  /** The listener that may receive callbacks. */
  private OnFragmentInteractionListener listener;
  
  private Button playButton;
  private LinearLayout difficultyBtnsLayout;
  
  /** The list of buttons which when selected will start the game at a certain difficulty. */
  private List<Button> difficultyBtns = new ArrayList<>();
  
  /**
   * The list of the animations that make the difficulty buttons slide up from the bottom. This is
   * a list because the "fromYDelta" and "duration" attributes will vary depending on the button.
   */
  private List<Animation> slideUpAnims = new ArrayList<>();
  
  private boolean difficultyBtnsShowing = false;
  
  // For the achievement for toggling so many times
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  private int timesToggled = 0;
  private boolean awardedAchievement = false;
  
  /** Required empty public constructor. */
  public DifficultyButtonsFragment() {}
  
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    
    if (context instanceof OnFragmentInteractionListener) {
      listener = (OnFragmentInteractionListener) context;
    } else {
      // Default to a base no-op listener
      listener = difficulty -> {};
    }
    
    if (hideText == null) {
      // default to "close" - in onAttach() because in onInflate() it may not be attached yet
      hideText = getString(R.string.close);
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (getArguments() != null) {
      showText = getArguments().getString(ARG_SHOW_TEXT);
      hideText = getArguments().getString(ARG_HIDE_TEXT);
    }
  
    connection = new GPGSHelperServiceConnection(this);
  }
  
  /** Handle the fragment's custom attributes: showText and hideText. */
  @Override
  public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs, Bundle savedInstanceState) {
    super.onInflate(context, attrs, savedInstanceState);
    
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DifficultyButtonsFragment);
    
    String newShowText = a.getString(R.styleable.DifficultyButtonsFragment_show_text);
    if (newShowText != null) {
      showText = newShowText;
    }
    
    String newHideText = a.getString(R.styleable.DifficultyButtonsFragment_hide_text);
    if (newHideText != null) {
      hideText = newHideText;
    }
    
    a.recycle();
  }
  
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_difficulty_buttons, container, false);
    
    difficultyBtnsLayout = view.findViewById(R.id.difficulty_button_layout);
    
    playButton = view.findViewById(R.id.play_button);
    playButton.setText(showText);
    playButton.setOnClickListener(this);
    
    // Initialize difficultyBtns
    Collections.addAll(difficultyBtns,
        view.findViewById(R.id.easy_button),
        view.findViewById(R.id.normal_button),
        view.findViewById(R.id.hard_button)
    );
    
    for (Button button : difficultyBtns) {
      button.setOnClickListener(this);
    }
    
    final RelativeLayout layout = view.findViewById(R.id.difficulty_button_fragment_layout);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      public void onGlobalLayout() {
        // Initialize slideUpAnims with properly initialized animations.
        
        int slideUpSpeed = getResources().getInteger(R.integer.slide_up_speed);
        
        int[] playButtonLocation = new int[2];
        int[] buttonLocation = new int[2];
        
        playButton.getLocationOnScreen(playButtonLocation);
        int playButtonY = playButtonLocation[1];
        
        for (int i = 0; i < difficultyBtns.size(); i++) {
          Button button = difficultyBtns.get(i);
          
          button.getLocationOnScreen(buttonLocation);
          int buttonY = buttonLocation[1];
          
          float fromYDelta = Math.abs(buttonY - playButtonY);
          float duration = Util.pxToDp(getResources(), fromYDelta) / 100 * slideUpSpeed;
          
          TranslateAnimation anim = new TranslateAnimation(0, 0, fromYDelta, 0);
          anim.setInterpolator(new DecelerateInterpolator());
          anim.setDuration((long) duration);
          anim.setFillAfter(true);
          
          slideUpAnims.add(anim);
        }
        
        // Don't call this OnGlobalLayoutListener another time
        layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
      }
    });
    
    return view;
  }
  
  @Override
  public void onStart() {
    super.onStart();
    
    // Connected to the GPGSHelperService
    Intent intent = new Intent(getContext(), GPGSHelperService.class);
    requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
  }
  
  @Override
  public void onStop() {
    super.onStop();
    requireContext().unbindService(connection);
  }
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
    gpgsHelper.signInSilently(getActivity());
  }
  
  @Override
  public void onResume() {
    super.onResume();
    timesToggled = 0; // reset timesToggled for this 'sitting'
    
    if (gpgsHelper != null) {
      gpgsHelper.signInSilently(getActivity());
    }
  }
  
  @Override
  public void onPause() {
    super.onPause();
    if (difficultyBtnsShowing) {
      // Don't show the difficulty buttons if the user presses back after clicking on one of them
      // The animations still play because there's a slight delay before launching GameActivity
      hideDifficultyButtons();
    }
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    listener = null;
  }
  
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    
    // Clean up references
    playButton = null;
    difficultyBtnsLayout = null;
    difficultyBtns.clear(); // hopefully this cleans up properly
    slideUpAnims.clear(); // this too
  }
  
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.play_button:
        toggleDifficultyButtons();
        break;
      case R.id.easy_button:
        toGameActivity(0);
        break;
      case R.id.normal_button:
        toGameActivity(1);
        break;
      case R.id.hard_button:
        toGameActivity(2);
        break;
    }
  }
  
  /** @return Whether the sliding up animations are playing, using the first (longest) animation. */
  private boolean areSlidingUpAnimationsPlaying() {
    return !slideUpAnims.get(0).hasStarted() || slideUpAnims.get(0).hasEnded();
  }
  
  private void toggleDifficultyButtons() {
    if (areSlidingUpAnimationsPlaying()) {
      if (difficultyBtnsShowing) {
        hideDifficultyButtons();
      } else {
        showDifficultyButtons();
      }
    }
  }
  
  private void showDifficultyButtons() {
    difficultyBtnsShowing = true;
    
    difficultyBtnsLayout.setVisibility(View.VISIBLE);
    
    for (int i = 0; i < difficultyBtns.size(); i++) {
      Button button = difficultyBtns.get(i);
      Animation anim = slideUpAnims.get(i);
      
      // Undo reversing from hideDifficultyButtons()
      if (anim.getInterpolator() instanceof ReverseInterpolator) {
        anim.setInterpolator(((ReverseInterpolator) anim.getInterpolator()).getDelegate());
      }
      
      button.startAnimation(anim);
    }
    
    playButton.setText(hideText);
    
    ((TransitionDrawable) playButton.getBackground()).startTransition(
        getResources().getInteger(R.integer.play_button_transition_ms));
  }
  
  private void hideDifficultyButtons() {
    difficultyBtnsShowing = false;
    
    for (int i = 0; i < difficultyBtns.size(); i++) {
      Button button = difficultyBtns.get(i);
      Animation anim = slideUpAnims.get(i);
      
      // Reverse the animation
      anim.setInterpolator(new ReverseInterpolator(anim.getInterpolator()));
      
      button.startAnimation(anim);
    }
    
    // Make the difficulty buttons invisible when the animations finish
    // slideUpAnims[0] is the longest so we use it to detect the animations' finishing
    slideUpAnims.get(0).setAnimationListener(new Animation.AnimationListener() {
      public void onAnimationStart(Animation animation) {}
      public void onAnimationRepeat(Animation animation) {}
      
      public void onAnimationEnd(Animation animation) {
        difficultyBtnsLayout.setVisibility(View.INVISIBLE);
        slideUpAnims.get(0).setAnimationListener(null);
      }
    });
    
    playButton.setText(showText);
  
    ((TransitionDrawable) playButton.getBackground()).reverseTransition(
        getResources().getInteger(R.integer.play_button_transition_ms));
    
    // There's an achievement for toggling the difficulty buttons so many times
    if (!awardedAchievement) {
      timesToggled++;
      if (timesToggled >= getResources().getInteger(R.integer.achievement_play_with_drawer_times)) {
        gpgsHelper.unlockAchievement(R.string.achievement_id_play_with_drawer);
        awardedAchievement = true;
      }
    }
  }
  
  /** Go to {@link GameActivity}, setting the difficulty extra with the parameter. */
  private void toGameActivity(int difficulty) {
    Intent intent = new Intent(getActivity(), GameActivity.class);
    intent.putExtra(PostGameActivity.EXTRA_DIFFICULTY, difficulty);
    
    startActivity(intent, Util.getActivityTransition(getActivity()));
    listener.afterToGameActivity(difficulty);
  }
  
  /**
   * This interface may be implemented by activities which contain this fragment in order to allow
   * communication between the fragment and the activity; namely, through a hook in order to execute
   * code after a button has been pressed.
   */
  interface OnFragmentInteractionListener {
    void afterToGameActivity(int difficulty);
  }
  
}