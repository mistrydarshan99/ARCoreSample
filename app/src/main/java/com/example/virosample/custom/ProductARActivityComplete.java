/*
 * Copyright (c) 2017-present, Viro, Inc.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.virosample.custom;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.virosample.R;
import com.example.virosample.ViroActivity;
import com.example.virosample.ViroHelper;
import com.viro.core.ARAnchor;
import com.viro.core.ARHitTestListener;
import com.viro.core.ARHitTestResult;
import com.viro.core.ARNode;
import com.viro.core.ARScene;
import com.viro.core.AmbientLight;
import com.viro.core.AnimationTimingFunction;
import com.viro.core.AnimationTransaction;
import com.viro.core.AsyncObject3DListener;
import com.viro.core.ClickListener;
import com.viro.core.ClickState;
import com.viro.core.DragListener;
import com.viro.core.GestureRotateListener;
import com.viro.core.Material;
import com.viro.core.Node;
import com.viro.core.Object3D;
import com.viro.core.Portal;
import com.viro.core.PortalScene;
import com.viro.core.RendererStartListener;
import com.viro.core.RotateState;
import com.viro.core.Spotlight;
import com.viro.core.Surface;
import com.viro.core.Texture;
import com.viro.core.Vector;
import com.viro.core.ViroMediaRecorder;
import com.viro.core.ViroViewARCore;
import java.io.File;
import java.util.Arrays;

/**
 * A ViroCore ProductARActivity that coordinates the placing of a Product last selected in the
 * in AR.
 */
public class ProductARActivityComplete extends ViroActivity implements RendererStartListener {
  final public static String INTENT_PRODUCT_KEY = "product_key";
  private View mHudGroupView = null;
  private TextView mHUDInstructions;
  private ImageView mCameraButton;
  private View mIconShakeView;
  private ARScene arScene;
  private PortalScene portalScene;

  /*
   The Tracking status is used to coordinate the displaying of our 3D controls and HUD
   UI as the user looks around the tracked AR Scene.
   */
  private enum TRACK_STATUS {
    FINDING_SURFACE, SURFACE_NOT_FOUND, SURFACE_FOUND, SELECTED_SURFACE;
  }

  private TRACK_STATUS mStatus = TRACK_STATUS.SURFACE_NOT_FOUND;
  private Node mProductModelGroup = null;
  private Node mCrosshairModel = null;
  private AmbientLight mMainLight = null;
  private Vector mLastProductRotation = new Vector();
  private Vector mSavedRotateToRotation = new Vector();
  private ARHitTestListenerCrossHair mCrossHairHitTest = null;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    String key = intent.getStringExtra(INTENT_PRODUCT_KEY);

    View.inflate(this, R.layout.ar_hud, ((ViewGroup) mViroView));
    mHudGroupView = (View) findViewById(R.id.main_hud_layout);
    mHudGroupView.setVisibility(View.GONE);
  }

  @Override protected void onDestroy() {
    ((ViroViewARCore) mViroView).setCameraARHitTestListener(null);
    super.onDestroy();
  }

  @Override public void onRendererStart() {
    // Create the ARScene within which to load our ProductAR Experience
    arScene = new ARScene();
    mMainLight = new AmbientLight(Color.parseColor("#606060"), 400);
    //mMainLight.setInfluenceBitMask(3);
    arScene.getRootNode().addLight(mMainLight);

    // Setup our 3D and HUD controls
    initARCrossHair(arScene);
    //init3DModelProduct(arScene);

    initARHud();

    // Start our tracking UI when the scene is ready to be tracked
    arScene.setListener(new ARSceneListener());

    // Finally set the arScene on the renderer
    mViroView.setScene(arScene);
  }

  private void initARHud() {
    // TextView instructions
    mHUDInstructions = (TextView) mViroView.findViewById(R.id.ar_hud_instructions);
    mViroView.findViewById(R.id.bottom_frame_controls).setVisibility(View.VISIBLE);

    // Bind the back button on the top left of the layout
    ImageView view = (ImageView) findViewById(R.id.ar_back_button);
    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        ProductARActivityComplete.this.finish();
      }
    });

    // Bind the detail buttons on the top right of the layout.
    ImageView productDetails = (ImageView) findViewById(R.id.ar_details_page);
    productDetails.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
      }
    });

    // Bind the camera button on the bottom, for taking images.
    mCameraButton = (ImageView) mViroView.findViewById(R.id.ar_photo_button);
    final File photoFile = new File(getFilesDir(), "screenShot");
    mCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mViroView.getRecorder()
            .takeScreenShotAsync("screenShot", true,
                new ViroMediaRecorder.ScreenshotFinishListener() {
                  @Override public void onSuccess(Bitmap bitmap, String s) {
                    final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(s));
                    startActivity(Intent.createChooser(shareIntent, "Share image using"));
                  }

                  @Override public void onError(ViroMediaRecorder.Error error) {
                    Log.e("Viro", "onTaskFailed " + error.toString());
                  }
                });
      }
    });

    mIconShakeView = mViroView.findViewById(R.id.icon_shake_phone);
  }

  private void initARCrossHair(ARScene scene) {
    if (mCrosshairModel != null) {
      return;
    }

    final Object3D crossHairModel = new Object3D();
    scene.getRootNode().addChildNode(crossHairModel);
    crossHairModel.loadModel(Uri.parse("file:///android_asset/tracking_1.vrx"), Object3D.Type.FBX,
        new AsyncObject3DListener() {
          @Override public void onObject3DLoaded(Object3D object3D, Object3D.Type type) {

            mCrosshairModel = object3D;
            mCrosshairModel.setOpacity(0);
            mCrosshairModel.setScale(new Vector(0.175, 0.175, 0.175));
            mCrosshairModel.setClickListener(new ClickListener() {
              @Override public void onClick(int i, Node node, Vector vector) {
                Log.e("Demo", "onClick: -------------------------------------");
                setTrackingStatus(TRACK_STATUS.SELECTED_SURFACE);
              }

              @Override
              public void onClickState(int i, Node node, ClickState clickState, Vector vector) {
                // No-op
              }
            });
          }

          @Override public void onObject3DFailed(String error) {
            Log.e("Viro", " Model load failed : " + error);
          }
        });
  }

  private void init3DModelProduct(ARScene scene) {
    // Create our group node containing the light, shadow plane, and 3D models
    mProductModelGroup = new Node();

    // Create a light to be shined on the model.
    Spotlight spotLight = new Spotlight();
    spotLight.setInfluenceBitMask(1);
    spotLight.setPosition(new Vector(0, 5, 0));
    spotLight.setCastsShadow(true);
    spotLight.setAttenuationEndDistance(7);
    spotLight.setAttenuationStartDistance(4);
    spotLight.setDirection(new Vector(0, -1, 0));
    spotLight.setIntensity(6000);
    spotLight.setShadowOpacity(0.35f);
    mProductModelGroup.addLight(spotLight);

    // Create a mock shadow plane in AR
    Node shadowNode = new Node();
    Surface shadowSurface = new Surface(20, 20);
    Material material = new Material();
    material.setShadowMode(Material.ShadowMode.TRANSPARENT);
    material.setLightingModel(Material.LightingModel.LAMBERT);
    shadowSurface.setMaterials(Arrays.asList(material));
    shadowNode.setGeometry(shadowSurface);
    shadowNode.setLightReceivingBitMask(1);
    shadowNode.setPosition(new Vector(0, -0.01, 0));
    shadowNode.setRotation(new Vector(-1.5708, 0, 0));
    mProductModelGroup.addChildNode(shadowNode);

    // Load the model from the given mSelected Product
    final Object3D productModel = new Object3D();
    productModel.loadModel(Uri.parse("file:///android_asset/object_lamp.vrx"), Object3D.Type.FBX,
        new AsyncObject3DListener() {
          @Override public void onObject3DLoaded(Object3D object3D, Object3D.Type type) {
            object3D.setLightReceivingBitMask(1);
            mProductModelGroup.setOpacity(0);
            mProductModelGroup.setScale(new Vector(0.9, 0.9, 0.9));
            mLastProductRotation = object3D.getRotationEulerRealtime();
          }

          @Override public void onObject3DFailed(String error) {
            Log.e("Viro", " Model load failed : " + error);
          }
        });

    // Make this 3D Product object draggable.
    mProductModelGroup.setDragType(Node.DragType.FIXED_TO_WORLD);
    mProductModelGroup.setDragListener(new DragListener() {
      @Override public void onDrag(int i, Node node, Vector vector, Vector vector1) {
        // No-op
      }
    });

    // Set click listeners on this 3D product
    productModel.setClickListener(new ClickListener() {
      @Override public void onClick(int i, Node node, Vector vector) {
        // No-op
      }

      @Override public void onClickState(int i, Node node, ClickState clickState, Vector vector) {
        onModelClick(clickState);
      }
    });

    // Set gesture listeners such that the user can rotate this model.
    productModel.setGestureRotateListener(new GestureRotateListener() {
      @Override
      public void onRotate(int source, Node node, float radians, RotateState rotateState) {
        Vector rotateTo = new Vector(mLastProductRotation.x, mLastProductRotation.y + radians,
            mLastProductRotation.z);
        productModel.setRotation(rotateTo);
        mSavedRotateToRotation = rotateTo;
      }
    });

    mProductModelGroup.setOpacity(0);
    mProductModelGroup.addChildNode(productModel);
    scene.getRootNode().addChildNode(mProductModelGroup);
  }

  private void setTrackingStatus(TRACK_STATUS status) {
    if (mStatus == TRACK_STATUS.SELECTED_SURFACE || mStatus == status) {
      Log.e("Demo", "setTrackingStatus: -----------------------------Return");
      return;
    }

    // If the surface has been selected, we no longer need our cross hair listener.
    if (status == TRACK_STATUS.SELECTED_SURFACE) {
      ((ViroViewARCore) mViroView).setCameraARHitTestListener(null);
      Log.e("Demo", "setTrackingStatus: -----------------------------listner null");
    }

    mStatus = status;
    updateUIHud();
    update3DARCrosshair();
    update3DModelProduct();
  }

  private void updateUIHud() {
    switch (mStatus) {
      case FINDING_SURFACE:
        mHUDInstructions.setText(
            "Point the camera at the flat surface where you want to view your product.");
        break;
      case SURFACE_NOT_FOUND:
        mHUDInstructions.setText(
            "We can’t seem to find a surface. Try moving your phone more in any direction.");
        break;
      case SURFACE_FOUND:
        mHUDInstructions.setText("Great! Now tap where you want to see the product.");
        break;
      case SELECTED_SURFACE:
        mHUDInstructions.setText("Great! Use one finger to move and two fingers to rotate.");
        break;
      default:
        mHUDInstructions.setText("Initializing AR....");
    }

    // Update the camera UI
    if (mStatus == TRACK_STATUS.SELECTED_SURFACE) {
      mCameraButton.setVisibility(View.VISIBLE);
    } else {
      mCameraButton.setVisibility(View.GONE);
    }

    // Update the Icon shake view
    if (mStatus == TRACK_STATUS.SURFACE_NOT_FOUND) {
      mIconShakeView.setVisibility(View.VISIBLE);
    } else {
      mIconShakeView.setVisibility(View.GONE);
    }
  }

  private void update3DARCrosshair() {
    switch (mStatus) {
      case FINDING_SURFACE:
      case SURFACE_NOT_FOUND:
      case SELECTED_SURFACE:
        mCrosshairModel.setOpacity(0);
        break;
      case SURFACE_FOUND:
        mCrosshairModel.setOpacity(1);
        break;
    }

    if (mStatus == TRACK_STATUS.SELECTED_SURFACE && mCrossHairHitTest != null) {
      mCrossHairHitTest = null;
      ((ViroViewARCore) mViroView).setCameraARHitTestListener(null);
    } else if (mCrossHairHitTest == null) {
      mCrossHairHitTest = new ARHitTestListenerCrossHair();
      ((ViroViewARCore) mViroView).setCameraARHitTestListener(mCrossHairHitTest);
    }
  }

  private void update3DModelProduct() {
    // Hide the product if the user has not placed it yet.
   /* if (mStatus != TRACK_STATUS.SELECTED_SURFACE) {
      mProductModelGroup.setOpacity(0);
      return;
    }

    Vector position = mCrosshairModel.getPositionRealtime();
    Vector rotation = mCrosshairModel.getRotationEulerRealtime();

    mProductModelGroup.setOpacity(1);
    mProductModelGroup.setPosition(position);
    mProductModelGroup.setRotation(rotation);*/

    if (mStatus != TRACK_STATUS.SELECTED_SURFACE) {
      Log.e("Demo", "update3DModelProduct: ----------------------------------return>");
      return;
    }
    Vector position = mCrosshairModel.getPositionRealtime();
    Log.e("Demo", "update3DModelProduct: -------------------------DOne------------------------->");
    setUpPortalView(position);
  }

  private void onModelClick(ClickState state) {
    if (state == ClickState.CLICK_DOWN) {
      mLastProductRotation = mProductModelGroup.getRotationEulerRealtime();
    } else if (state == ClickState.CLICK_UP) {
      mLastProductRotation = mSavedRotateToRotation;
    }
  }

  private class ARHitTestListenerCrossHair implements ARHitTestListener {
    @Override public void onHitTestFinished(ARHitTestResult[] arHitTestResults) {
      if (arHitTestResults == null || arHitTestResults.length <= 0) {
        return;
      }

      // If we have found intersected AR Hit points, update views as needed, reset miss count.
      ViroViewARCore viewARView = (ViroViewARCore) mViroView;
      final Vector cameraPos = viewARView.getLastCameraPositionRealtime();

      // Grab the closest ar hit target
      float closestsDistance = Float.MAX_VALUE;
      ARHitTestResult result = null;
      for (int i = 0; i < arHitTestResults.length; i++) {
        ARHitTestResult currentResult = arHitTestResults[i];

        float distance = currentResult.getPosition().distance(cameraPos);
        if (distance < closestsDistance && distance > .3 && distance < 5) {
          result = currentResult;
          closestsDistance = distance;
        }
      }

      // Update the cross hair target location with the closest target.
      animateCrossHairToPosition(result);

      // Update State based on hit target
      if (result != null) {
        setTrackingStatus(TRACK_STATUS.SURFACE_FOUND);
      } else {
        setTrackingStatus(TRACK_STATUS.FINDING_SURFACE);
      }
    }

    private void animateCrossHairToPosition(ARHitTestResult result) {
      if (result == null) {
        return;
      }

      AnimationTransaction.begin();
      AnimationTransaction.setAnimationDuration(70);
      AnimationTransaction.setTimingFunction(AnimationTimingFunction.EaseOut);
      mCrosshairModel.setPosition(result.getPosition());
      mCrosshairModel.setRotation(result.getRotation());
      AnimationTransaction.commit();
    }
  }

  protected class ARSceneListener implements ARScene.Listener {
    @Override public void onTrackingInitialized() {
      // The Renderer is ready - turn everything visible.
      mHudGroupView.setVisibility(View.VISIBLE);

      // Update our ui views to the finding surface state.
      setTrackingStatus(TRACK_STATUS.FINDING_SURFACE);
    }

    @Override public void onAmbientLightUpdate(float lightIntensity, float colorTemperature) {
      // no-op
    }

    @Override public void onAnchorFound(ARAnchor anchor, ARNode arNode) {
      // no-op
    }

    @Override public void onAnchorUpdated(ARAnchor anchor, ARNode arNode) {
      // no-op
    }

    @Override public void onAnchorRemoved(ARAnchor anchor, ARNode arNode) {
      // no-op
    }
  }

  private void setUpPortalView(Vector vectorPosition) {
    Log.e("Demo", "setUpPortalView: -------------------------------------load portal------------>");
    // Load a model representing the ship door
    Object3D shipDoorModel = new Object3D();
    shipDoorModel.loadModel(Uri.parse("file:///android_asset/portal_ship.vrx"), Object3D.Type.FBX,
        null);

    // Create a Portal out of the ship door
    Portal portal = new Portal();
    portal.addChildNode(shipDoorModel);
    portal.setScale(new Vector(0.5, 0.5, 0.5));

    // Create a PortalScene that uses the Portal as an entrance.
    portalScene = new PortalScene();
    //portalScene.setPosition(new Vector(0, 0, -3));
    portalScene.setPosition(vectorPosition);

    portalScene.setPassable(true);
    portalScene.setPortalEntrance(portal);

    // Add a 'beach' background for the Portal scene
    final Bitmap beachBackground = ViroHelper.getBitmapFromAsset(this, "beach.jpg");
    final Texture beachTexture = new Texture(beachBackground, Texture.Format.RGBA8, true, false);
    portalScene.setBackgroundTexture(beachTexture);

    arScene.getRootNode().addChildNode(portalScene);
  }
}
