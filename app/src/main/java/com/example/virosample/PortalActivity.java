package com.example.virosample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.viro.core.ARAnchor;
import com.viro.core.ARNode;
import com.viro.core.ARScene;
import com.viro.core.Object3D;
import com.viro.core.OmniLight;
import com.viro.core.Portal;
import com.viro.core.PortalScene;
import com.viro.core.RendererStartListener;
import com.viro.core.Texture;
import com.viro.core.Vector;
import com.viro.core.ViroViewARCore;
import java.lang.ref.WeakReference;

public class PortalActivity extends AppCompatActivity implements RendererStartListener {

  private ViroViewARCore mViroView;
  //private ViroViewGVR mViroView;
  private ARScene mScene;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mViroView = new ViroViewARCore(this, this);
    /*mViroView = new ViroViewGVR(this, this, new Runnable() {
      @Override public void run() {
        // Handle existing GVR Here
      }
    });*/
    //mViroView.setVRModeEnabled(true);
    setContentView(mViroView);
    setUpScene();
  }

  @Override public void onRendererStart() {
    //setUpScene();

  }

  private void setUpScene() {
    mScene = new ARScene();
    //add a listener to the scene so we can update 'AR Init' text.
    mScene.setListener(new PortalActivity.ARSceneListener(this, mViroView));

    // Add a Light so the ship door portal entrance will be visible
    OmniLight light = new OmniLight();
    light.setColor(Color.WHITE);
    light.setPosition(new Vector(0, 1, -4));
    mScene.getRootNode().addLight(light);

    mViroView.setScene(mScene);
    //View.inflate(this, R.layout.viro_view_ar_hit_test_hud, ((ViewGroup) mViroView));
    View.inflate(this, R.layout.layout_demo, ((ViewGroup) mViroView));
    setUpPortalView();
  }

  private void setUpPortalView() {


    // Load a model representing the ship door
    Object3D shipDoorModel = new Object3D();
    shipDoorModel.loadModel(Uri.parse("file:///android_asset/portal_ship.vrx"), Object3D.Type.FBX,
        null);

    // Create a Portal out of the ship door
    Portal portal = new Portal();
    portal.addChildNode(shipDoorModel);
    portal.setScale(new Vector(0.5, 0.5, 0.5));

    // Create a PortalScene that uses the Portal as an entrance.
    PortalScene portalScene = new PortalScene();
    portalScene.setPosition(new Vector(0, 0, -3));
    portalScene.setPassable(true);
    portalScene.setPortalEntrance(portal);

    // Add a 'beach' background for the Portal scene
    final Bitmap beachBackground = ViroHelper.getBitmapFromAsset(this, "beach.jpg");
    final Texture beachTexture = new Texture(beachBackground, Texture.Format.RGBA8, true, false);
    portalScene.setBackgroundTexture(beachTexture);

    mScene.getRootNode().addChildNode(portalScene);
  }

  @Override protected void onStart() {
    super.onStart();
    mViroView.onActivityStarted(this);
  }

  @Override protected void onResume() {
    super.onResume();
    mViroView.onActivityResumed(this);
  }

  @Override protected void onPause() {
    super.onPause();
    mViroView.onActivityPaused(this);
  }

  @Override protected void onStop() {
    super.onStop();
    mViroView.onActivityStopped(this);
  }

  /*
  Private class that implements ARScene.Listener callbacks. In this example we use this to notify the user
  AR is initialized.
  */
  private static class ARSceneListener implements ARScene.Listener {
    private WeakReference<Activity> mCurrentActivityWeak;

    public ARSceneListener(Activity activity, View rootView) {
      mCurrentActivityWeak = new WeakReference<Activity>(activity);
    }

    @Override public void onTrackingInitialized() {
      Activity activity = mCurrentActivityWeak.get();
      if (activity == null) {
        return;
      }

      TextView initText = (TextView) activity.findViewById(R.id.initText);
      initText.setText("AR is initialized.");
    }

    @Override public void onAmbientLightUpdate(float v, float v1) {

    }

    @Override public void onAnchorFound(ARAnchor arAnchor, ARNode arNode) {

    }

    @Override public void onAnchorRemoved(ARAnchor arAnchor, ARNode arNode) {

    }

    @Override public void onAnchorUpdated(ARAnchor arAnchor, ARNode arNode) {

    }
  }
}
