package com.camera.widget;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 *
 * File name: BaseActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.camera.usbcam.R;
import com.camera.utils.HandlerThreadHandler;

public class BaseActivity extends FragmentActivity
{
	private static boolean DEBUG = false;
	private static final String TAG = BaseActivity.class.getSimpleName();

	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private final Thread mUiThread = mUIHandler.getLooper().getThread();
	private Handler mWorkerHandler;
	private long mWorkerThreadID = -1;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mWorkerHandler == null) {
			mWorkerHandler = HandlerThreadHandler.createHandler(TAG);
			mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();
		}
	}

	@Override
	protected void onPause() {
		clearToast();
		super.onPause();
	}

	@Override
	protected synchronized void onDestroy() {
		if (mWorkerHandler != null) {
			try {
				mWorkerHandler.getLooper().quit();
			} catch (final Exception e) {
			}
			mWorkerHandler = null;
		}
		super.onDestroy();
	}

//================================================================================
	public final void runOnUiThread(final Runnable task, final long duration) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
		if ((duration > 0) || Thread.currentThread() != mUiThread) {
			mUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public final void removeFromUiThread(final Runnable task) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
	}

	protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
		if ((task == null) || (mWorkerHandler == null)) return;
		try {
			mWorkerHandler.removeCallbacks(task);
			if (delayMillis > 0) {
				mWorkerHandler.postDelayed(task, delayMillis);
			} else if (mWorkerThreadID == Thread.currentThread().getId()) {
				task.run();
			} else {
				mWorkerHandler.post(task);
			}
		} catch (final Exception e) {
			// ignore
		}
	}

	protected final synchronized void removeEvent(final Runnable task) {
		if (task == null) return;
		try {
			mWorkerHandler.removeCallbacks(task);
		} catch (final Exception e) {
			// ignore
		}
	}

//================================================================================
	private Toast mToast;
	protected void showToast(@StringRes final int msg, final Object... args) {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = new ShowToastTask(msg, args);
		runOnUiThread(mShowToastTask, 0);
	}

	protected void clearToast() {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = null;
		try {
			if (mToast != null) {
				mToast.cancel();
				mToast = null;
			}
		} catch (final Exception e) {
			// ignore
		}
	}

	private ShowToastTask mShowToastTask;
	private final class ShowToastTask implements Runnable {
		final int msg;
		final Object args;
		private ShowToastTask(@StringRes final int msg, final Object... args) {
			this.msg = msg;
			this.args = args;
		}

		@Override
		public void run() {
			try {
				if (mToast != null) {
					mToast.cancel();
					mToast = null;
				}
				if (args != null) {
					final String _msg = getString(msg, args);
					mToast = Toast.makeText(BaseActivity.this, _msg, Toast.LENGTH_SHORT);
				} else {
					mToast = Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_SHORT);
				}
				mToast.show();
			} catch (final Exception e) {
				// ignore
			}
		}
	}

//================================================================================
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		final int n = Math.min(permissions.length, grantResults.length);
		for (int i = 0; i < n; i++) {
			checkPermissionResult(requestCode, permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
		}
	}

	protected void checkPermissionResult(final int requestCode, final String permission, final boolean result) {
		if (!result && (permission != null)) {
			if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
				showToast(R.string.permission_audio);
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
				showToast(R.string.permission_ext_storage);
			}
			if (Manifest.permission.INTERNET.equals(permission)) {
				showToast(R.string.permission_network);
			}
		}
	}
}
