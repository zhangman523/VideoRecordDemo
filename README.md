# VideoRecordDemo

Android视频录制 需要注意的几点。

- 初始化Camera 之后必须要先`unlock()` 然后再设置Camera 不然无法录制视频

- 录制完视频，释放MediaRecorder 的时候必须调用Camera的`lock()`方法 ,不然无法释放成功


## 录制视频步骤

第一步 初始化Camera
```
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
```

第二步 开始录制视频

```
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
```

第三步 停止录制

```
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
```

参考
-------

[android-MediaRecorder](https://github.com/googlesamples/android-MediaRecorder)

[android-Camera2Basic](https://github.com/googlesamples/android-Camera2Basic)

License
-------

Copyright 2017 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.