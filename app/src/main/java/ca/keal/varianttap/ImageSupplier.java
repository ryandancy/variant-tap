package ca.keal.varianttap;

import android.content.res.AssetManager;
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
class ImageSupplier {
  
  // TODO if performance is bad, maybe don't load all the images but instead load their filenames
  // TODO "packages" of images
  
  Random random = new Random();
  
  /**
   * The list of (normal, variant) Drawable pairs.
   */
  private List<Pair<Drawable, Drawable>> imgs = new ArrayList<>();
  
  /**
   * Initialize this {@link ImageSupplier} by loading the images from {@code assets}. There must be
   * a normals/ and variants/ directory in the assets directory, and their contents must have the
   * same names.
   * @param assets - An {@link AssetManager}, most likely accessed via {@code getAssets()}.
   */
  ImageSupplier(AssetManager assets) {
    try {
      load(assets);
      Log.i(getClass().getName(), imgs.size() + " normal/variant image pairs loaded successfully.");
    } catch (IOException e) {
      Log.e(getClass().getName(), "Error loading images: " + e);
      throw new RuntimeException(e); // UncheckedIOException is API Level 24, our min is 19
    }
  }
  
  private void load(AssetManager assets) throws IOException {
    // TODO should we check that normals/ and variants/ have same contents or just trust it?
    // Currently, if they aren't the same, the ones in normals/ will be loaded and if they aren't in
    // variants/ an IOException will be thrown. Any extra variants will be ignored.
    
    String[] names = assets.list("normals");
    
    for (String name : names) {
      imgs.add(Pair.create(
          Drawable.createFromPath("normals/" + name),
          Drawable.createFromPath("variants/" + name)
      ));
    }
  }
  
  /**
   * @return An unmodifiable {@link List} of all loaded normal/variant Drawable pairs.
   */
  List<Pair<Drawable, Drawable>> getAllPairs() {
    return Collections.unmodifiableList(imgs);
  }
  
  /**
   * @return A psuedorandom normal/variant Drawable pair. Please don't modify it.
   */
  Pair<Drawable, Drawable> getRandomPair() {
    return imgs.get(random.nextInt(imgs.size()));
  }
  
}