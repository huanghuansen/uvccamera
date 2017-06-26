package com.camera.showvedio;

import android.graphics.Bitmap;

public class BitmapEntity {
	private Bitmap bitmap;
	private String name;
	private String uri;
	private long size;
	private String uri_thumb;
	public BitmapEntity(String name, String uri, long size, String uri_thumb, long duration) {
		super();
		this.name = name;
		this.uri = uri;
		this.size = size;
		this.uri_thumb = uri_thumb;
		this.duration = duration;
	}

	public String getUri_thumb() {
		return uri_thumb;
	}

	public void setUri_thumb(String uri_thumb) {
		this.uri_thumb = uri_thumb;
	}
	private long duration;//持续时间
	public BitmapEntity(Bitmap bitmap, String name, String uri, long size, long duration) {
		super();
		this.bitmap = bitmap;
		this.name = name;
		this.uri = uri;
		this.size = size;
		this.duration = duration;
	}
	
	public BitmapEntity(Bitmap bitmap, String name, long size, long duration) {
		super();
		this.bitmap = bitmap;
		this.name = name;
		this.size = size;
		this.duration = duration;
	}

	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public BitmapEntity() {
		super();
	}
	public BitmapEntity(Bitmap bitmap, String name, String uri) {
		super();
		this.bitmap = bitmap;
		this.name = name;
		this.uri = uri;
	}
	public Bitmap getBitmap() {
		return bitmap;
	}
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}

}
