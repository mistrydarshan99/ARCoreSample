package com.example.virosample.custom;

import android.app.Activity;
import android.net.Uri;
import android.widget.Toast;
import com.viro.core.AsyncObject3DListener;
import com.viro.core.ClickListener;
import com.viro.core.ClickState;
import com.viro.core.DragListener;
import com.viro.core.GesturePinchListener;
import com.viro.core.GestureRotateListener;
import com.viro.core.Node;
import com.viro.core.Object3D;
import com.viro.core.PinchState;
import com.viro.core.RotateState;
import com.viro.core.Vector;

public class Draggable3dObject {

  private String mFileName;
  private float rotateStart;
  private float scaleStart;
  private Activity activity;

  public Draggable3dObject(String filename, Activity activity) {
    mFileName = filename;
    this.activity = activity;
  }

  public Object3D addModelToPosition(Vector position) {
    final Object3D object3D = new Object3D();
    object3D.setPosition(position);
    // Shrink the objects as the original size is too large.
    object3D.setScale(new Vector(.2f, .2f, .2f));
    object3D.setGestureRotateListener(new GestureRotateListener() {
      @Override public void onRotate(int i, Node node, float rotation, RotateState rotateState) {
        if (rotateState == RotateState.ROTATE_START) {
          rotateStart = object3D.getRotationEulerRealtime().y;
        }
        float totalRotationY = rotateStart + rotation;
        object3D.setRotation(new Vector(0, totalRotationY, 0));
      }
    });

    object3D.setGesturePinchListener(new GesturePinchListener() {
      @Override public void onPinch(int i, Node node, float scale, PinchState pinchState) {
        if (pinchState == PinchState.PINCH_START) {
          scaleStart = object3D.getScaleRealtime().x;
        } else {
          object3D.setScale(new Vector(scaleStart * scale, scaleStart * scale, scaleStart * scale));
        }
      }
    });

    object3D.setDragListener(new DragListener() {
      @Override public void onDrag(int i, Node node, Vector vector, Vector vector1) {

      }
    });
    object3D.setClickListener(new ClickListener() {
      @Override public void onClick(int i, Node node, Vector vector) {
      }

      @Override public void onClickState(int i, Node node, ClickState clickState, Vector vector) {

      }
    });

    // Load the Android model asynchronously.
    object3D.loadModel(Uri.parse(mFileName), Object3D.Type.FBX, new AsyncObject3DListener() {
      @Override public void onObject3DLoaded(final Object3D object, final Object3D.Type type) {
        //TODO: Display toast saying model loaded successfully.
      }

      @Override public void onObject3DFailed(String s) {
        Toast.makeText(activity, "An error occured when loading the 3d Object!", Toast.LENGTH_LONG)
            .show();
      }
    });

    // Make the object draggable.
    object3D.setDragType(Node.DragType.FIXED_TO_WORLD);
    return object3D;
  }
}
