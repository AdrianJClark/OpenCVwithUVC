/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.uvcpupildetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import com.serenegiant.usbcameracommon.UVCCameraHandlerMultiSurface;
import com.serenegiant.uvcpupildetector.MyCallbackListener;
import com.serenegiant.widget.UVCCameraTextureView;

/**
 * Show side by side view from two camera.
 * You cane record video images from both camera, but secondarily started recording can not record
 * audio because of limitation of Android AudioRecord(only one instance of AudioRecord is available
 * on the device) now.
 */
public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
	private static final boolean DEBUG = true;	// FIXME set false when production
	private static final String TAG = "MainActivity";

	private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

	// for accessing USB and USB camera
	private USBMonitor mUSBMonitor;

	//private UVCCameraHandler mHandlerR;
	//private SurfaceView mUVCCameraViewR;
	//private Surface mRightPreviewSurface;
	//MyCallbackListener frameCallbackR = null;

	private UVCCameraHandler mHandlerL;
	private UVCCameraTextureView mUVCCameraViewL;
	private Surface mLeftPreviewSurface;
	MyCallbackListener frameCallbackL = null;

	private boolean settingCameraLeft = true;

	private ToggleButton mCameraButton;
	//protected SurfaceView mResultView;

	private static final boolean USE_SURFACE_ENCODER = false;
	private static final int PREVIEW_WIDTH = 640;
	private static final int PREVIEW_HEIGHT = 480;
	private static final int PREVIEW_MODE = 1;

	private UVCCameraHandlerMultiSurface mCameraHandler;

	PyObject pythonModule;
	ImageView imageView;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);

		setContentView(R.layout.activity_main);
		mCameraButton = findViewById(R.id.camera_button);
		mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);

		mUVCCameraViewL = findViewById(R.id.camera_view);
		//mUVCCameraViewL.setOnLongClickListener(mOnLongClickListener);
		mUVCCameraViewL.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

		//mResultView = findViewById(R.id.result_view);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
		mCameraHandler = UVCCameraHandlerMultiSurface.createHandler(this, mUVCCameraViewL,
				USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

		//mUVCCameraViewL = mUnityPlayer.getView();
		mHandlerL = UVCCameraHandler.createHandler(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0]);

		//mUVCCameraViewR = mUnityPlayer.getView();
		//mHandlerR = UVCCameraHandler.createHandler(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1]);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

		if (!Python.isStarted()) {
			Python.start(new AndroidPlatform(this));
		}

		Python python = Python.getInstance();
		pythonModule = python.getModule("process_image"); // Points to process_image.py

		imageView = this.findViewById(R.id.result_view);

//		Bitmap inputBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_image);
//
//		//  Compress Bitmap to Byte Array
//		//ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		//inputBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
//		//byte[] byteArray = outputStream.toByteArray();
//
//		ByteBuffer buffer = ByteBuffer.allocate(inputBitmap.getByteCount());
//		// Copy the bitmap pixels into the buffer
//		inputBitmap.copyPixelsToBuffer(buffer);
//		byte[] byteArray = buffer.array();
//
//		// Call Python function
//		PyObject pyObject = pythonModule.callAttr("convert_to_grayscale", byteArray, inputBitmap.getWidth(), inputBitmap.getHeight(), 4);
//
//		// Retrieve returned raw bytes and re-convert to an Android Bitmap
//		byte[] processedBytes = pyObject.toJava(byte[].class);
//		Bitmap resultBitmap = BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.length);
//
//		// Display the processed image in your layout
//		imageView.setImageBitmap(resultBitmap);
	}

	private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
			= new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(
				final CompoundButton compoundButton, final boolean isChecked) {

			if (compoundButton.getId() == R.id.camera_button) {
				if (isChecked && !mHandlerL.isOpened()) {
					CameraDialog.showDialog(MainActivity.this);
				} else {
					//stopPreview();
					mCameraHandler.close();
				}
			}
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		mUSBMonitor.register();
	}

	@Override
	protected void onStop() {
		//mHandlerR.close();
		mHandlerL.close();
		mUSBMonitor.unregister();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		//if (mHandlerR != null) {
		//	mHandlerR = null;
		//}
		if (mHandlerL != null) {
			mHandlerL = null;
		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		//mUVCCameraViewR = null;
		mUVCCameraViewL = null;
		super.onDestroy();
	}

	public void registerFrameCallbackL(MyCallbackListener frameCallback) {
		frameCallbackL = frameCallback;
	}

	//public void registerFrameCallbackR(MyCallbackListener frameCallback) {
	//	frameCallbackR = frameCallback;
	//}


	public void onClickL() {
		if (mHandlerL != null) {
			if (!mHandlerL.isOpened()) {
				CameraDialog.showDialog(MainActivity.this);
				settingCameraLeft = true;
			} else {
				mHandlerL.close();
			}
		}
	}

	/*public void onClickR() {
		if (mHandlerR != null) {
			if (!mHandlerR.isOpened()) {
				CameraDialog.showDialog(MainActivity.this);
				settingCameraLeft = false;
			} else {
				mHandlerR.close();
			}
		}
	}*/

	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onAttach:" + device);
			Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.v(TAG, "onConnect:" + device);
			if (settingCameraLeft) {
				if (!mHandlerL.isOpened()) {
					mHandlerL.open(ctrlBlock);
					mHandlerL.setFrameCallback(mIFrameCallbackL, UVCCamera.PIXEL_FORMAT_RGBX);
					mLeftPreviewSurface = mUVCCameraViewL./*getHolder().*/getSurface();
					mHandlerL.startPreview(mLeftPreviewSurface);
				}
			} else {
				/*if (!mHandlerR.isOpened()) {
					mHandlerR.open(ctrlBlock);
					mHandlerR.setFrameCallback(mIFrameCallbackR, UVCCamera.PIXEL_FORMAT_RGBX);
					mRightPreviewSurface = mUVCCameraViewR.getHolder().getSurface();
					mHandlerR.startPreview(mRightPreviewSurface);
				}*/
			}
		}

		private final IFrameCallback mIFrameCallbackL = new IFrameCallback() {
			@Override
			public void onFrame(final ByteBuffer frame) {

				try {
					byte[] byteArray = new byte[frame.remaining()];
					frame.get(byteArray);
					PyObject pyObject = pythonModule.callAttr("convert_to_grayscale", byteArray, 640, 480, 4);

					byte[] processedBytes = pyObject.toJava(byte[].class);
					Bitmap resultBitmap = BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.length);
					imageView.setImageBitmap(resultBitmap);
					
				} catch (Exception e) {
					Log.v(TAG, "Received Frame Callback e:" + e);
				}
			}
		};

		/*private final IFrameCallback mIFrameCallbackR = new IFrameCallback() {
			@Override
			public void onFrame(final ByteBuffer frame) {
				if (frameCallbackR!=null) {
					frameCallbackR.onFrame(frame);
				}
			}
		};*/

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
			if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
				/*queueEvent(new Runnable() {
					@Override
					public void run() {*/
				mHandlerL.close();
				if (mLeftPreviewSurface != null) {
					mLeftPreviewSurface.release();
					mLeftPreviewSurface = null;
				}
				//	setCameraButton();
					/*}
				}, 0);*/
			}/* else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
				/ *queueEvent(new Runnable() {
					@Override
					public void run() {* /
						mHandlerR.close();
						if (mRightPreviewSurface != null) {
							mRightPreviewSurface.release();
							mRightPreviewSurface = null;
						}
					//	setCameraButton();
				/ *	}
				}, 0);* /
			}*/
		}

		@Override
		public void onDettach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onDettach:" + device);
			Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onCancel:");
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (canceled) {
			/*runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setCameraButton();
				}
			}, 0);*/
		}
	}

}
