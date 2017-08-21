package ca.keal.varianttap;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
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

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * A {@link Fragment} which contains a button that shows the difficulty buttons in order to choose
 * the difficulty. Activities that contain this fragment may implement the
 * {@link DifficultyButtonsFragment.OnFragmentInteractionListener} interface to handle interaction
 * events. Use the {@link DifficultyButtonsFragment#newInstance} factory method (or one of its
 * sister {@code newInstance()} methods) to create an instance of this fragment.
 */
public class DifficultyButtonsFragment extends Fragment implements View.OnClickListener {
  
  // Transition direction constants
  public static final int DIRECTION_LEFT = 0;
  public static final int DIRECTION_RIGHT = 1;
  
  // The fragment initialization parameters
  private static final String ARG_SHOW_TEXT = "SHOW_TEXT";
  private static final String ARG_HIDE_TEXT = "HIDE_TEXT";
  
  // Fragment parameters
  private String showText = null;
  private String hideText = null;
  private int transitionDirection = -1;
  
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
  
  /** Required empty public constructor. */
  public DifficultyButtonsFragment() {}
  
  /**
   * Create an instance of this fragment using the specified parameters.
   * @param showText the string shown on the play button when tapping will show the buttons.
   * @param hideText the string shown on the play button when tapping will hide the buttons.
   * @return An instance of this fragment.
   */
  public static DifficultyButtonsFragment newInstance(String showText, String hideText) {
    DifficultyButtonsFragment fragment = new DifficultyButtonsFragment();
    Bundle args = new Bundle();
    args.putString(ARG_SHOW_TEXT, showText);
    args.putString(ARG_HIDE_TEXT, hideText);
    fragment.setArguments(args);
    return fragment;
  }
  
  /** Like {@link #newInstance(String, String)}, but uses resource IDs instead. */
  public static DifficultyButtonsFragment newInstance(
      Context context, @StringRes int showText, @StringRes int hideText) {
    return newInstance(context.getString(showText), context.getString(hideText));
  }
  
  /**
   * Like {@link #newInstance(String, String)}, but {@code showText} defaults to
   * R.string.close_difficulty_btns.
   */
  public static DifficultyButtonsFragment newInstance(Context context, String showText) {
    return newInstance(showText, context.getString(R.string.close_difficulty_btns));
  }
  
  /**
   * Like {@link #newInstance(Context, int, int)}, but {@code showText} defaults to
   * R.string.close_difficulty_btns.
   */
  public static DifficultyButtonsFragment newInstance(Context context, @StringRes int showText) {
    return newInstance(context, showText, R.string.close_difficulty_btns);
  }
  
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    
    if (context instanceof OnFragmentInteractionListener) {
      listener = (OnFragmentInteractionListener) context;
    } else {
      // Default to a base no-op listener
      listener = new OnFragmentInteractionListener() {
        public void afterToGameActivity(int difficulty) {}
      };
    }
    
    if (hideText == null) {
      // default to "close" - in onAttach() because in onInflate() it may not be attached yet
      hideText = getString(R.string.close_difficulty_btns);
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      showText = getArguments().getString(ARG_SHOW_TEXT);
      hideText = getArguments().getString(ARG_HIDE_TEXT);
    }
  }
  
  /** Handle the fragment's custom attributes: showText and hideText. */
  @Override
  public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
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
    
    transitionDirection = a.getInt(
        R.styleable.DifficultyButtonsFragment_transition_direction, DIRECTION_RIGHT);
    
    a.recycle();
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_difficulty_buttons, container, false);
    
    //noinspection ConstantConditions
    difficultyBtnsLayout = (LinearLayout) view.findViewById(R.id.difficulty_button_layout);
    
    playButton = (Button) view.findViewById(R.id.play_button);
    playButton.setText(showText);
    playButton.setOnClickListener(this);
    
    // Initialize difficultyBtns
    Collections.addAll(difficultyBtns,
        (Button) view.findViewById(R.id.easy_button),
        (Button) view.findViewById(R.id.normal_button),
        (Button) view.findViewById(R.id.hard_button)
    );
    
    for (Button button : difficultyBtns) {
      button.setOnClickListener(this);
    }
    
    RelativeLayout layout = (RelativeLayout)
        view.findViewById(R.id.difficulty_button_fragment_layout);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      public void onGlobalLayout() {
        // Initialize slideUpAnims with properly initialized animations.
        // This is in an OnGlobalLayoutListener so that the calls to getY() happen after the layout
        // is actually laid out (if this was in the main method, getY() would return 0).
        
        int slideUpSpeed = getResources().getInteger(R.integer.slide_up_speed);
        
        for (int i = 0; i < difficultyBtns.size(); i++) {
          Button button = difficultyBtns.get(i);
          
          float fromYDelta = Math.abs(button.getY() - (playButton.getY() - playButton.getHeight()));
          float duration = Util.pxToDp(getResources(), fromYDelta) / 100 * slideUpSpeed;
          
          TranslateAnimation anim = new TranslateAnimation(0, 0, fromYDelta, 0);
          anim.setInterpolator(new DecelerateInterpolator());
          anim.setDuration((long) duration);
          anim.setFillAfter(true);
          
          slideUpAnims.add(anim);
        }
      }
    });
    
    return view;
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
  }
  
  /** Go to {@link GameActivity}, setting the difficulty extra with the parameter. */
  private void toGameActivity(int difficulty) {
    Intent intent = new Intent(getActivity(), GameActivity.class);
    intent.putExtra(PostGameActivity.EXTRA_DIFFICULTY, difficulty);
    
    // Handle the transition direction
    Bundle options;
    if (transitionDirection == DIRECTION_LEFT) {
      options = Util.getToLeftTransition(getActivity());
    } else { // transitionDirection == DIRECTION_RIGHT
      options = Util.getToRightTransition(getActivity());
    }
    
    startActivity(intent, options);
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