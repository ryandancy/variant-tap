package ca.keal.varianttap.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Loads the normal and variant images and provides Drawables of random normal/variant pairs when
 * requested.
 */
public class ImageSupplier {
  
  // TODO "packages" of images
  
  private static final String TAG = "ImageSupplier";
  
  private static ImageSupplier instance = null;
  
  public Random random = new Random();
  
  private List<String> imgNames; // initialized in load()
  private List<String> currentlyPreloading = new ArrayList<>();
  private Map<String, Pair<Drawable, Drawable>> loaded = new HashMap<>();
  
  private AssetManager assets;
  private Resources resources;
  
  /**
   * Get the global ImageSupplier instance, creating it if necessary.
   * @param context The Context from which a {@link Resources} and {@link AssetManager} can be
   *                retrieved if necessary.
   * @return The global ImageSupplier instance.
   */
  public static ImageSupplier getInstance(Context context) {
    if (instance == null) {
      instance = new ImageSupplier(context.getAssets(), context.getResources());
    }
    return instance;
  }
  
  /**
   * Initialize this {@link ImageSupplier} by loading the names of the images from {@code assets}.
   * There must be a normals/ and variants/ directory in the assets directory, and their contents
   * must have the same names. For internal use.
   */
  private ImageSupplier(AssetManager assets, Resources resources) {
    this.assets = assets;
    this.resources = resources;
    
    try {
      // TODO should we check that normals/ and variants/ have same contents or just trust it?
      imgNames = Arrays.asList(Objects.requireNonNull(assets.list("normals")));
      Log.i(TAG, "Found " + imgNames.size() + " normal/variant image pairs.");
    } catch (IOException | NullPointerException e) {
      Log.e(TAG, "Error loading images filenames: " + e);
      throw new RuntimeException(e); // UncheckedIOException is API Level 24, our min is 19
    }
  }
  
  /**
   * Preload random images for later use. If {@code toUnloadAfter} is specified, unload images after
   * the others have been loaded. Preloading is done asynchronously.
   * @param upTo The number of images that will be loaded after preloading and unloading is done.
   *             The number of loaded images will be "topped up" to this amount.
   * @param toUnloadAfter Optionally, filenames to be unloaded from memory after preloading.
   */
  public void preload(int upTo, String... toUnloadAfter) {
    int howMany = upTo + toUnloadAfter.length - loaded.size() - currentlyPreloading.size();
    if (howMany == 0) {
      return;
    } else if (howMany < 0) {
      // Unload however many needed randomly so we don't get stuck in a situation with no new images
      int toUnload = Math.min(loaded.size(), -howMany);
      Log.w(TAG, "Attempted to preload " + howMany + " images, unloading " + toUnload);
      
      List<String> loadedNames = new ArrayList<>(loaded.keySet());
      if (toUnloadAfter.length > 0) {
        loadedNames.removeAll(Arrays.asList(toUnloadAfter));
      }
      Collections.shuffle(loadedNames);
      
      unload(loadedNames.subList(0, toUnload).toArray(new String[toUnload]));
      
      // parameters calculated so that howMany = 0 to avoid infinite recursion if something goes bad
      preload(loaded.size() + currentlyPreloading.size() - toUnloadAfter.length, toUnloadAfter);
      return;
    }
    
    // Get a list of the non-preloaded images
    List<String> nonPreloaded = new ArrayList<>();
    for (String imgName : imgNames) {
      if (!loaded.containsKey(imgName) && !currentlyPreloading.contains(imgName)) {
        nonPreloaded.add(imgName);
      }
    }
    
    if (nonPreloaded.size() < howMany) {
      Log.w(TAG, "Attempted to preload more images (" + howMany + ") than not already preloaded ("
        + nonPreloaded.size() + "); " + loaded.size() + " images already preloaded.");
      howMany = nonPreloaded.size();
      if (howMany == 0) return;
    }
    
    // Get a random sublist to preload
    Collections.shuffle(nonPreloaded);
    List<String> toPreload = nonPreloaded.subList(0, howMany);
    String[] toPreloadArray = toPreload.toArray(new String[0]);
    
    // Preload them
    currentlyPreloading.addAll(toPreload);
    new PreloadImagesTask(toUnloadAfter).execute(toPreloadArray);
  }
  
  @SuppressLint("StaticFieldLeak") // can't live longer than the class itself
  private class PreloadImagesTask extends AsyncTask<String, Void, Void> {
    
    private String[] toUnloadAfter;
    
    PreloadImagesTask(String... toUnloadAfter) {
      this.toUnloadAfter = toUnloadAfter;
    }
    
    @Override
    protected Void doInBackground(String... filenames) {
      for (String filename : filenames) {
        loadFilename(filename);
      }
      return null;
    }
    
    private void loadFilename(String filename) {
      try {
        loaded.put(filename, loadPair(filename));
        Log.v(TAG, "Loaded: " + filename);
      } catch (IOException e) {
        Log.e(TAG, "Failed to load \"" + filename + "\" images", e);
      } catch (OutOfMemoryError e) {
        Log.e(TAG, "Ran out of memory!", e);
      
        // try unloading toUnloadAfter first
        if (toUnloadAfter.length > 0) {
          unload(toUnloadAfter);
          toUnloadAfter = new String[0];
          loadFilename(filename);
        } else {
          Log.e(TAG, "Skipping loading \"" + filename + "\" due to lack of memory!");
        }
      } finally {
        currentlyPreloading.remove(filename);
      }
    }
    
    @Override
    protected void onPostExecute(Void v) {
      if (toUnloadAfter.length > 0) {
        unload(toUnloadAfter);
      }
    }
    
  }
  
  private Pair<Drawable, Drawable> loadPair(String filename) throws IOException {
    return Pair.create(
        Drawable.createFromResourceStream(
            resources, null, assets.open("normals/" + filename), null),
        Drawable.createFromResourceStream(
            resources, null, assets.open("variants/" + filename), null)
    );
  }
  
  private void unload(String... toUnload) {
    for (String filename : toUnload) {
      Log.v(TAG, "Unloaded: " + filename);
      loaded.remove(filename);
    }
    System.gc(); // to remove the pair of drawables that was unloaded
  }
  
  /**
   * @return A pseudorandom image name and normal/variant Drawable pair. Please don't modify it.
   */
  public Pair<String, Pair<Drawable, Drawable>> getRandomPair() {
    // Holy Generics, Batman!
    List<Map.Entry<String, Pair<Drawable, Drawable>>> entries = new ArrayList<>(loaded.entrySet());
    
    if (entries.size() == 0) {
      Log.e(TAG, "No images available to get!");
      throw new IllegalStateException("No images available to get!");
    }
    
    Map.Entry<String, Pair<Drawable, Drawable>> entry = entries.get(random.nextInt(entries.size()));
    return Pair.create(entry.getKey(), entry.getValue());
  }
  
  /**
   * @return A pseudorandom image name and normal or variant. Please don't modify it.
   */
  public Pair<String, Drawable> getRandomImage() {
    Pair<String, Pair<Drawable, Drawable>> nameAndPossible = getRandomPair();
    Pair<Drawable, Drawable> possible = nameAndPossible.second;
    return Pair.create(nameAndPossible.first,
        random.nextBoolean() ? possible.first : possible.second);
  }
  
  /**
   * Get the image pair with the specified name. If the image pair is not loaded, load it
   * synchronously.
   * @param name The name of the image pair to be retrieved.
   * @return The image pair with the specified name.
   */
  public Pair<Drawable, Drawable> getPairByName(String name) {
    if (loaded.containsKey(name)) {
      return loaded.get(name);
    } else if (imgNames.contains(name)) {
      try {
        // this will block, but it shouldn't be that bad as it's only 1 pair
        return loadPair(name);
      } catch (IOException e) {
        Log.e(TAG, "Failed to load \"" + name + "\" images", e);
        throw new RuntimeException("Failed to load requested images", e);
      }
    } else {
      Log.e(TAG, "Nonexistent name " + name + " requested!");
      throw new IllegalArgumentException("Nonexistent name " + name + " requested!");
    }
  }
  
}