package com.droidprojects.spotifytray;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

public class Utils {
	
	/**
	 * Takes an inputstream, loads a bitmap optimized of space and then crops it for the view.
	 * @param iStream Inputstream to the image.
	 * @param containerHeight Height of the image holder container. 
	 * @param containerWidth Width of the image holder container.
	 * @return Cropped/masked bitmap.
	 */
	public static Bitmap loadMaskedBitmap(InputStream iStream, int containerHeight, int containerWidth){
		
		// Load image data before loading the image itself.
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		
		BitmapFactory.decodeStream(iStream,null,bmOptions);
		
		int photoH = bmOptions.outHeight;
		int photoW = bmOptions.outWidth;
		
		// Find a suitable scalefactor to load the smaller bitmap, and then set the options.
		int scaleFactor = Math.min(photoH/containerHeight, photoW/containerHeight);
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		// Load the square region out of the bitmap.
		Bitmap bmap=null;
		BitmapRegionDecoder decoder;
		try {
			iStream.reset();
			decoder = BitmapRegionDecoder.newInstance(iStream, false);
			bmap = decoder.decodeRegion(new Rect(0, 0, Math.min(photoH, photoW), Math.min(photoH, photoW)),bmOptions);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Calculate new width of the bitmap based on the width of the container
		int bitmapNewWidth = (bmap.getWidth()*containerWidth)/containerHeight;   
		
		// Produce clipping mask on the canvas and draw the bitmap 
		Bitmap targetBitmap = Bitmap.createBitmap(bitmapNewWidth, bmap.getHeight(), bmap.getConfig());
		Canvas canvas = new Canvas(targetBitmap);
		Path path = new Path();
		path.addCircle(bmap.getWidth() / 2, bmap.getHeight() / 2, bmap.getWidth() / 2, Path.Direction.CCW);
		canvas.clipPath(path);
		canvas.drawBitmap(bmap, 0, 0, null);

		// Retrieve the clipped bitmap and return
		return targetBitmap;
	}
	
	/**
	 * Converts dps to pixels.
	 * @param dp Value in dp.
	 * @param res Resources reference to get the screen density.
	 * @return Value in pixels.
	 */
	public static int dpToPixels(int dp, Resources res){
		return (int)(res.getDisplayMetrics().density*dp + 0.5f);
	}
}
