package com.example.virosample.custom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.virosample.R;
import com.viro.core.ARAnchor;
import com.viro.core.ARHitTestListener;
import com.viro.core.ARHitTestResult;
import com.viro.core.ARNode;
import com.viro.core.ARScene;
import com.viro.core.AmbientLight;
import com.viro.core.Object3D;
import com.viro.core.RendererStartListener;
import com.viro.core.Vector;
import com.viro.core.ViroViewARCore;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DemoActivity extends AppCompatActivity implements RendererStartListener {

  private static final String TAG = DemoActivity.class.getSimpleName();
  private ViroViewARCore mViroView;
  static final float MIN_DISTANCE = .2f;
  static final float MAX_DISTANCE = 10f;
  /*
 Reference to the arScene we will be creating within this activity
 */
  private ARScene mScene;

  /*
   List of draggable 3d objects in our scene.
   */
  private List<Draggable3dObject> mDraggableObjects;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mDraggableObjects = new ArrayList<>();
    mViroView = new ViroViewARCore(this, this);
    setContentView(mViroView);
  }

  private void setUpScene() {
    mScene = new ARScene();
    //add a listener to the scene so we can update 'AR Init' text.
    mScene.setListener(new ARSceneListener(this, mViroView));
    //add a light to the scene so our models can show up.
    mScene.getRootNode().addLight(new AmbientLight(Color.WHITE, 1000f));
    mViroView.setScene(mScene);
    //View.inflate(this, R.layout.viro_view_ar_hit_test_hud, ((ViewGroup) mViroView));
    View.inflate(this, R.layout.layout_demo, ((ViewGroup) mViroView));
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

  /*
  *Dialog menu of virtual objects we can place in the real world.
  */
  public void showPopup(View v) {

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    CharSequence itemsList[] = { "Coffee mug", "Flowers", "Smile Emoji" };
    builder.setTitle("Choose an object").setItems(itemsList, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        switch (which) {
          case 0:
            placeObject("file:///android_asset/object_coffee_mug.vrx");
            break;
          case 1:
            placeObject("file:///android_asset/object_flowers.vrx");
            break;
          case 2:
            placeObject("file:///android_asset/emoji_smile.vrx");
            break;
        }
      }
    });

    Dialog d = builder.create();
    d.show();
  }

  private void placeObject(final String fileName) {
    ViroViewARCore viewARView = (ViroViewARCore) mViroView;
    final Vector cameraPos = viewARView.getLastCameraPositionRealtime();
    viewARView.performARHitTestWithRay(viewARView.getLastCameraForwardRealtime(),
        new ARHitTestListener() {
          @Override public void onHitTestFinished(ARHitTestResult[] arHitTestResults) {
            if (arHitTestResults != null) {
              if (arHitTestResults.length > 0) {
                for (int i = 0; i < arHitTestResults.length; i++) {
                  ARHitTestResult result = arHitTestResults[i];
                  float distance = result.getPosition().distance(cameraPos);
                  if (distance > MIN_DISTANCE && distance < MAX_DISTANCE) {
                    // If we found a plane of feature point greater than .2 and less than 10 meters away
                    // then choose it!
                    add3dDraggableObject(fileName, result.getPosition());
                    return;
                  }
                }
              }
            }
            Toast.makeText(DemoActivity.this,
                "Unable to find suitable point or plane to place object!", Toast.LENGTH_LONG)
                .show();
          }
        });
  }

  /*
    Add a 3d object with the given filename to the scene at the specified world position.
   */
  private void add3dDraggableObject(String filename, Vector position) {
    Draggable3dObject draggable3dObject = new Draggable3dObject(filename, this);
    mDraggableObjects.add(draggable3dObject);
    Object3D object3D = draggable3dObject.addModelToPosition(position);
    mScene.getRootNode().addChildNode(object3D);
  }

  public void removeObject(){
    mScene.getRootNode().notify();
  }

  @Override public void onRendererStart() {
    setUpScene();
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
}
