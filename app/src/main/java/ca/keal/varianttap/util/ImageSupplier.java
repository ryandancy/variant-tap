package ca.keal.varianttap.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Loads the normal and variant images and provides Drawables of random normal/variant pairs when
 * requested.
 */
public class ImageSupplier {
  
  // TODO "packages" of images
  
  private static String TAG = "ImageSupplier";
  
  private static ImageSupplier instance = null;
  
  public Random random = new Random();
  
  /**
   * The list of (normal, variant) Drawable pairs.
   */
  private List<Pair<Drawable, Drawable>> imgs = new ArrayList<>();
  
  /**
   * Get the global ImageSupplier instance, creating it if necessary.
   * @param context The Context that an AssetManager can be gotten from if necessary.
   * @return the global ImageSupplier instance.
   */
  public static ImageSupplier getInstance(Context context) {
    if (instance == null) {
      instance = new ImageSupplier(context.getAssets(), context.getResources());
    }
    return instance;
  }
  
  /**
   * Initialize this {@link ImageSupplier} by loading the images from {@code assets}. There must be
   * a normals/ and variants/ directory in the assets directory, and their contents must have the
   * same names. For internal use.
   * @param assets An {@link AssetManager}, most likely accessed via {@code getAssets()}.
   */
  private ImageSupplier(AssetManager assets, Resources resources) {
    try {
      load(assets, resources);
      Log.i(TAG, imgs.size() + " normal/variant image pairs loaded successfully.");
    } catch (IOException e) {
      Log.e(TAG, "Error loading images: " + e);
      throw new RuntimeException(e); // UncheckedIOException is API Level 24, our min is 19
    }
  }
  
  private void load(AssetManager assets, Resources resources) throws IOException {
    // TODO should we check that normals/ and variants/ have same contents or just trust it?
    // Currently, if they aren't the same, the ones in normals/ will be loaded and if they aren't in
    // variants/ an IOException will be thrown. Any extra variants will be ignored.
    
    String[] names = assets.list("normals");
    
    for (String name : names) {
      imgs.add(Pair.create(
          Drawable.createFromResourceStream(resources, null, assets.open("normals/" + name), null),
          Drawable.createFromResourceStream(resources, null, assets.open("variants/" + name), null)
      ));
    }
  }
  
  /**
   * @return An unmodifiable {@link List} of all loaded normal/variant Drawable pairs.
   */
  public List<Pair<Drawable, Drawable>> getAllPairs() {
    return Collections.unmodifiableList(imgs);
  }
  
  /**
   * @return A psuedorandom normal/variant Drawable pair. Please don't modify it.
   */
  public Pair<Drawable, Drawable> getRandomPair() {
    return imgs.get(random.nextInt(imgs.size()));
  }
  
  /**
   * @return A random normal or variant. Please don't modify it.
   */
  public Drawable getRandomImage() {
    Pair<Drawable, Drawable> possible = getRandomPair();
    return random.nextBoolean() ? possible.first : possible.second;
  }
  
  public int getPairId(Pair<Drawable, Drawable> pair) {
    int idx = imgs.indexOf(pair);
    if (idx < 0) {
      Log.w(TAG, "getPairId() passed nonexistent Drawable pair");
    }
    return idx;
  }
  
  public Pair<Drawable, Drawable> getPairById(int id) {
    return imgs.get(id);
  }
  
}