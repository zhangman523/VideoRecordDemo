package zhangman.github.videorecorddemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private final static String TAG = MainActivity.class.getSimpleName();
  //Requesting permission
  private boolean permissionToRecordAccepted = false;
  private String[] permission = {
      Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
  };
  private final int REQUEST_PERMISSION = 0x12;

  private Camera mCamera;
  private TextureView mPreview;
  private MediaRecorder mMediaRecorder;
  private File mOutputFile;
  private boolean isRecording = false;
  private Button captureButton;
  private CamcorderProfile profile;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    captureButton = findViewById(R.id.button_capture);
    mPreview = findViewById(R.id.surface_view);

    if (ActivityCompat.checkSelfPermission(this, permission[0]) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(this, permission[1])
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, permission, REQUEST_PERMISSION);
    } else {
    }
    mPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
      @Override
      public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startPreview();
      }

      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

      }

      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
      }

      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surface) {

      }
    });
    captureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startRecord();
      }
    });
  }

  private void startRecord() {
    if (isRecording) {
      // BEGIN_INCLUDE(stop_release_media_recorder)

      // stop recording and release camera
      try {
        mMediaRecorder.stop();  // stop the recording
      } catch (RuntimeException e) {
        // RuntimeException is thrown when stop() is called immediately after start().
        // In this case the output file is not properly constructed ans should be deleted.
        Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
        //noinspection ResultOfMethodCallIgnored
        mOutputFile.delete();
      }
      releaseMediaRecorder(); // release the MediaRecorder object
      mCamera.lock();         // take camera access back from MediaRecorder

      // inform the user that recording has stopped
      setCaptureButtonText("Capture");
      isRecording = false;
      releaseCamera();
      // END_INCLUDE(stop_release_media_recorder)

    } else {

      // BEGIN_INCLUDE(prepare_start_media_recorder)

      new MediaPrepareTask().execute(null, null, null);

      // END_INCLUDE(prepare_start_media_recorder)

    }
  }

  private void startPreview() {
    previewCamera();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_PERMISSION:
        permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        break;
    }
    if (!permissionToRecordAccepted) {
      Toast.makeText(this, "请允许所需权限", Toast.LENGTH_SHORT).show();
    }
  }

  private boolean previewCamera() {
    // BEGIN_INCLUDE (configure_preview)
    mCamera = CameraHelper.getDefaultCameraInstance();

    // We need to make sure that our preview and recording video size are supported by the
    // camera. Query camera to find all the sizes and choose the optimal size given the
    // dimensions of our preview surface.
    android.hardware.Camera.Parameters parameters = mCamera.getParameters();
    List<android.hardware.Camera.Size> mSupportedPreviewSizes =
        parameters.getSupportedPreviewSizes();
    List<android.hardware.Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
    android.hardware.Camera.Size optimalSize =
        CameraHelper.getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes,
            mPreview.getWidth(), mPreview.getHeight());

    // Use the same size for recording profile.
    profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    profile.videoFrameWidth = optimalSize.width;
    profile.videoFrameHeight = optimalSize.height;

    // likewise for the camera object itself.
    parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
    mCamera.setParameters(parameters);
    setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
    try {
      // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
      // with {@link SurfaceView}
      mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
      mCamera.startPreview();
    } catch (IOException e) {
      Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
      return false;
    }
    // END_INCLUDE (configure_preview)
    return true;
  }

  private boolean prepareVideoRecorder() {
    if (!previewCamera()) {
      return false;
    }
    // BEGIN_INCLUDE (configure_media_recorder)
    mMediaRecorder = new MediaRecorder();

    // Step 1: Unlock and set camera to MediaRecorder
    mCamera.unlock();
    mMediaRecorder.setCamera(mCamera);

    // Step 2: Set sources
    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
    mMediaRecorder.setProfile(profile);

    // Step 4: Set output file
    mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
    if (mOutputFile == null) {
      return false;
    }
    mMediaRecorder.setOutputFile(mOutputFile.getPath());
    // END_INCLUDE (configure_media_recorder)

    // Step 5: Prepare configured MediaRecorder
    try {
      mMediaRecorder.prepare();
    } catch (IllegalStateException e) {
      Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
      releaseMediaRecorder();
      return false;
    } catch (IOException e) {
      Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
      releaseMediaRecorder();
      return false;
    }
    return true;
  }

  public void setCameraDisplayOrientation(Activity activity, int cameraId,
      android.hardware.Camera camera) {
    android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
    android.hardware.Camera.getCameraInfo(cameraId, info);
    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }

    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    camera.setDisplayOrientation(result);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // if we are using MediaRecorder, release it first
    releaseMediaRecorder();
    // release the camera immediately on pause event
    releaseCamera();
  }

  private void releaseCamera() {
    if (mCamera != null) {
      // release the camera for other applications
      mCamera.release();
      mCamera = null;
    }
  }

  private void releaseMediaRecorder() {
    if (mMediaRecorder != null) {
      // clear recorder configuration
      mMediaRecorder.reset();
      // release the recorder object
      mMediaRecorder.release();
      mMediaRecorder = null;
      // Lock camera for later use i.e taking it back from MediaRecorder.
      // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
      mCamera.lock();
    }
  }

  private void setCaptureButtonText(String title) {
    captureButton.setText(title);
  }

  /**
   * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long
   * blocking
   * operation.
   */
  class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... voids) {
      // initialize video camera
      if (prepareVideoRecorder()) {
        // Camera is available and unlocked, MediaRecorder is prepared,
        // now you can start recording
        mMediaRecorder.start();

        isRecording = true;
      } else {
        // prepare didn't work, release the camera
        releaseMediaRecorder();
        return false;
      }
      return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      if (!result) {
        MainActivity.this.finish();
      }
      // inform the user that recording has started
      setCaptureButtonText("Stop");
    }
  }
}
