package com.camera.videotest2;

import android.graphics.Bitmap;

public class Picture {
	
	private Bitmap bitmap;
	private String path;
	private String name;



	public Bitmap getBitmap() {
		return bitmap;
	}
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	public String getPath() {
		return path;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Picture(Bitmap bitmap, String path) {
		super();
		this.bitmap = bitmap;
		this.path = path;
	}
	public Picture() {
		super();
		// TODO Auto-generated constructor stub
	}

}
