package com.example.virosample.custom;

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
import com.example.virosample.R;
import com.example.virosample.ViroHelper;
import com.viro.core.ARAnchor;
import com.viro.core.ARNode;
import com.viro.core.ARPlaneAnchor;
import com.viro.core.ARScene;
import com.viro.core.ClickListener;
import com.viro.core.ClickState;
import com.viro.core.Material;
import com.viro.core.Node;
import com.viro.core.Object3D;
import com.viro.core.OmniLight;
import com.viro.core.Portal;
import com.viro.core.PortalScene;
import com.viro.core.RendererStartListener;
import com.viro.core.Surface;
import com.viro.core.Texture;
import com.viro.core.Vector;
import com.viro.core.ViroViewARCore;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PortalActivity extends AppCompatActivity implements RendererStartListener {

  private ViroViewARCore mViroView;
  private ARScene mScene;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mViroView = new ViroViewARCore(this, this);
    setContentView(mViroView);
  }

  @Override public void onRendererStart() {
    setUpScene();

  }

  private void setUpScene() {
    mScene = new ARScene();
    mScene.displayPointCloud(false);
    // Create an TrackedPlanesController to visually display tracked planes
    PortalActivity.TrackedPlanesController controller =
        new PortalActivity.TrackedPlanesController(this, mViroView);

    // Spawn a 3D Droid on the position where the user has clicked on a tracked plane.
    controller.addOnPlaneClickListener(new ClickListener() {
      @Override public void onClick(int i, Node node, Vector clickPosition) {
        createDroidAtPosition(clickPosition);
        mViroView.setRenderStartListener(null);
      }

      @Override public void onClickState(int i, Node node, ClickState clickState, Vector vector) {
        //No-op
      }
    });
    mScene.setListener(controller);

    mViroView.setScene(mScene);
  }

  private void createDroidAtPosition(Vector clickPosition) {
    // Add a Light so the ship door portal entrance will be visible
    OmniLight light = new OmniLight();
    light.setColor(Color.WHITE);
    light.setPosition(new Vector(0, 1, -4));
    mScene.getRootNode().addLight(light);
    setUpPortalView(clickPosition);
  }

  private void setUpPortalView(Vector clickPosition) {

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
    //portalScene.setPosition(new Vector(0, 0, -5));
    portalScene.setPosition(clickPosition);
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

  private static class TrackedPlanesController implements ARScene.Listener {
    private WeakReference<Activity> mCurrentActivityWeak;
    private boolean searchingForPlanesLayoutIsVisible = false;
    private HashMap<String, Node> surfaces = new HashMap<String, Node>();
    private Set<ClickListener> mPlaneClickListeners = new HashSet<ClickListener>();

    public TrackedPlanesController(Activity activity, View rootView) {
      mCurrentActivityWeak = new WeakReference<Activity>(activity);

      // Inflate viro_view_hud.xml layout to display a "Searching for surfaces" text view.
      View.inflate(activity, R.layout.viro_view_hud, ((ViewGroup) rootView));
    }

    /**
     * Register click listener for other components to listen for click events that occur
     * on tracked planes. In this example, a listener is registered during scene creation,
     * so as spawn 3d droids on a click.
     */
    public void addOnPlaneClickListener(ClickListener listener) {
      mPlaneClickListeners.add(listener);
    }

    public void removeOnPlaneClickListener(ClickListener listener) {
      if (mPlaneClickListeners.contains(listener)) {
        mPlaneClickListeners.remove(listener);
      }
    }

    /**
     * Once a Tracked plane is found, we can hide the our "Searching for Surfaces" UI.
     */
    private void hideIsTrackingLayoutUI() {
      if (searchingForPlanesLayoutIsVisible) {
        return;
      }
      searchingForPlanesLayoutIsVisible = true;

      Activity activity = mCurrentActivityWeak.get();
      if (activity == null) {
        return;
      }

      View isTrackingFrameLayout = activity.findViewById(R.id.viro_view_hud);
      isTrackingFrameLayout.animate().alpha(0.0f).setDuration(2000);
    }

    @Override public void onAnchorFound(ARAnchor arAnchor, ARNode arNode) {
      // Spawn a visual plane if a PlaneAnchor was found
      if (arAnchor.getType() == ARAnchor.Type.PLANE) {
        ARPlaneAnchor planeAnchor = (ARPlaneAnchor) arAnchor;

        // Create the visual geometry representing this plane
        Vector dimensions = planeAnchor.getExtent();
        Surface plane = new Surface(1, 1);
        plane.setWidth(dimensions.x);
        plane.setHeight(dimensions.z);

        // Set a default material for this plane.
        Material material = new Material();
        material.setDiffuseColor(Color.parseColor("#BF000000"));
        plane.setMaterials(Arrays.asList(material));

        // Attach it to the node
        Node planeNode = new Node();
        planeNode.setGeometry(plane);
        planeNode.setRotation(new Vector(-Math.toRadians(90.0), 0, 0));
        planeNode.setPosition(planeAnchor.getCenter());

        // Attach this planeNode to the anchor's arNode
        arNode.addChildNode(planeNode);
        surfaces.put(arAnchor.getAnchorId(), planeNode);

        // Attach click listeners to be notified upon a plane onClick.
        planeNode.setClickListener(new ClickListener() {
          @Override public void onClick(int i, Node node, Vector vector) {
            for (ClickListener listener : mPlaneClickListeners) {
              listener.onClick(i, node, vector);
            }
          }

          @Override
          public void onClickState(int i, Node node, ClickState clickState, Vector vector) {
            //No-op
          }
        });

        // Finally, hide isTracking UI if we haven't done so already.
        hideIsTrackingLayoutUI();
      }
    }

    @Override public void onAnchorUpdated(ARAnchor arAnchor, ARNode arNode) {
      if (arAnchor.getType() == ARAnchor.Type.PLANE) {
        ARPlaneAnchor planeAnchor = (ARPlaneAnchor) arAnchor;

        // Update the mesh surface geometry
        Node node = surfaces.get(arAnchor.getAnchorId());
        Surface plane = (Surface) node.getGeometry();
        Vector dimensions = planeAnchor.getExtent();
        plane.setWidth(dimensions.x);
        plane.setHeight(dimensions.z);
      }
    }

    @Override public void onAnchorRemoved(ARAnchor arAnchor, ARNode arNode) {
      surfaces.remove(arAnchor.getAnchorId());
    }

    @Override public void onTrackingInitialized() {
      //No-op
    }

    @Override public void onAmbientLightUpdate(float v, float v1) {
      //No-op
    }
  }
}
