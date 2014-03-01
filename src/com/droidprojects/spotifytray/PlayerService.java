package com.droidprojects.spotifytray;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;







import com.example.serviceproject.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

@SuppressWarnings("deprecation")
public class PlayerService extends Service {

	// 
	private static final int TRAY_HIDDEN_FRACTION 			= 6; 	// Controls fraction of the tray hidden when open
	private static final int TRAY_MOVEMENT_REGION_FRACTION 	= 6;	// Controls fraction of y-axis on screen within which the tray stays.
	private static final int TRAY_CROP_FRACTION 			= 12;	// Controls fraction of the tray chipped at the right end.
	private static final int ANIMATION_FRAME_RATE 			= 30;	// Animation frame rate per second.
	private static final int TRAY_DIM_X_DP 					= 170;	// Width of the tray in dps
	private static final int TRAY_DIM_Y_DP 					= 160; 	// Height of the tray in dps
	private static final int BUTTONS_DIM_Y_DP 				= 27;	// Height of the buttons in dps
	
	// Layout containers for various widgets
	private WindowManager 				mWindowManager;			// Reference to the window
	private WindowManager.LayoutParams 	mRootLayoutParams;		// Parameters of the root layout
	private RelativeLayout 				mRootLayout;			// Root layout
	private RelativeLayout 				mContentContainerLayout;// Contains everything other than buttons and song info
	private RelativeLayout 				mLogoLayout;			// Contains Cpotify logo
	private RelativeLayout 				mAlbumCoverLayout;		// Contains album cover of the active song
	private RelativeLayout 				mAlbumCoverHelperLayout;// Contains cover of the previous song. This helps with fade animations.
	private LinearLayout 				mPlayerButtonsLayout;	// Contains playback buttons
	private LinearLayout 				mSongInfoLayout;		// Contains Text information on the current song

	// Variables that control drag
	private int mStartDragX;
	//private int mStartDragY; // Unused as yet
	private int mPrevDragX;
	private int mPrevDragY;
	
	private boolean mIsTrayOpen = true;
	
	// Controls for animations
	private Timer 					mTrayAnimationTimer;
	private TrayAnimationTimerTask 	mTrayTimerTask;
	private Handler 				mAnimationHandler = new Handler();
	
	// Mock images for album covers
	private ArrayList<String> 	mAlbumCoverImagePaths = new ArrayList<String>();
	private int 				mAlbumCoverImageIndex = 0;

	@Override
	public IBinder onBind(Intent intent) {
		// Not used
		return null;
	}

	@Override
	public void onCreate() {

		// Get references to all the views and add them to root view as needed.
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		mRootLayout = (RelativeLayout) LayoutInflater.from(this).
				inflate(R.layout.service_player, null);
		mContentContainerLayout = (RelativeLayout) mRootLayout.findViewById(R.id.content_container);
		mContentContainerLayout.setOnTouchListener(new TrayTouchListener());
		
		mLogoLayout = (RelativeLayout) mRootLayout.findViewById(R.id.logo_layout);
		mAlbumCoverLayout = (RelativeLayout) mRootLayout.findViewById(R.id.cover_layout);
		mAlbumCoverHelperLayout = (RelativeLayout) mRootLayout.findViewById(R.id.cover_helper_layout);
		
		mPlayerButtonsLayout = (LinearLayout) LayoutInflater.from(this).
				inflate(R.layout.viewgroup_player_buttons, null);
		mRootLayout.addView(mPlayerButtonsLayout);
		
		mSongInfoLayout = (LinearLayout) LayoutInflater.from(this).
				inflate(R.layout.viewgroup_song_info, null);
		mRootLayout.addView(mSongInfoLayout);
		
		mRootLayoutParams = new WindowManager.LayoutParams(
				Utils.dpToPixels(TRAY_DIM_X_DP, getResources()),
				Utils.dpToPixels(TRAY_DIM_Y_DP, getResources()),
				WindowManager.LayoutParams.TYPE_PHONE, 
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
				PixelFormat.TRANSLUCENT);

		mRootLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
		mWindowManager.addView(mRootLayout, mRootLayoutParams);
		
		populateAlbumCoverImages();
		
		// Since there is no function in a service like onWindowFocusChanged(), this function
		// is executed 50 milliseconds later. The main assumption is that all the layouts
		// have inflated and the widths/heights have been calculated within 50ms.
		mRootLayout.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				// Reusable variables
				RelativeLayout.LayoutParams params;
				InputStream is;
				Bitmap bmap;
				
				// Setup background spotify logo
				is = getResources().openRawResource(R.drawable.spot_bg);
				int containerNewWidth = (TRAY_CROP_FRACTION-1)*mLogoLayout.getHeight()/TRAY_CROP_FRACTION;
				bmap = Utils.loadMaskedBitmap(is, mLogoLayout.getHeight(), containerNewWidth);
				params = (RelativeLayout.LayoutParams) mLogoLayout.getLayoutParams();
				params.width = (bmap.getWidth() * mLogoLayout.getHeight()) / bmap.getHeight();
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,0);
				mLogoLayout.setLayoutParams(params);
				mLogoLayout.requestLayout();
				mLogoLayout.setBackgroundDrawable(new BitmapDrawable(getResources(), bmap));
				
				// Setup background album cover
				is=null;
				try {
					is = getAssets().open(mAlbumCoverImagePaths.get(mAlbumCoverImageIndex));
				} catch (IOException e) {
					e.printStackTrace();
				}
				bmap = Utils.loadMaskedBitmap(is, mAlbumCoverLayout.getHeight(), containerNewWidth);
				params = (RelativeLayout.LayoutParams) mAlbumCoverLayout.getLayoutParams();
				params.width = (bmap.getWidth() * mAlbumCoverLayout.getHeight()) / bmap.getHeight();
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,0);
				mAlbumCoverLayout.setLayoutParams(params);
				mAlbumCoverLayout.requestLayout();
				mAlbumCoverHelperLayout.setLayoutParams(params);
				mAlbumCoverHelperLayout.requestLayout();
				mAlbumCoverLayout.setBackgroundDrawable(new BitmapDrawable(getResources(), bmap));

				// Setup playback buttons
				params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT, 
						Utils.dpToPixels(BUTTONS_DIM_Y_DP, getResources()));
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.tray_opener);
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				params.leftMargin = mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION;
				mRootLayout.updateViewLayout(mPlayerButtonsLayout, params);
				
				// setup song info views
				params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT, 
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.tray_opener);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				int marg = Utils.dpToPixels(5, getResources());
				params.setMargins(
						marg/2 + mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION, 
						marg, 
						marg*3, 
						marg);
				mRootLayout.updateViewLayout(mSongInfoLayout, params);
				
				// Setup the root layout
				mRootLayoutParams.x = -mLogoLayout.getLayoutParams().width;
				mRootLayoutParams.y = (getApplicationContext().getResources().getDisplayMetrics().heightPixels-mRootLayout.getHeight()) / 2;
				mWindowManager.updateViewLayout(mRootLayout, mRootLayoutParams);
				
				// Make everything visible
				mRootLayout.setVisibility(View.VISIBLE);
				
				// Animate the Tray
				mTrayTimerTask = new TrayAnimationTimerTask();
				mTrayAnimationTimer = new Timer();
				mTrayAnimationTimer.schedule(mTrayTimerTask, 0, ANIMATION_FRAME_RATE);
			}
		}, ANIMATION_FRAME_RATE);
	}

	// The phone orientation has changed. Update the widget's position.
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mIsTrayOpen)
			mRootLayoutParams.x = -mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION;
		else
			mRootLayoutParams.x = -mLogoLayout.getWidth();
		mRootLayoutParams.y = (getResources().getDisplayMetrics().heightPixels-mRootLayout.getHeight()) / 2;
		mWindowManager.updateViewLayout(mRootLayout, mRootLayoutParams);
		animateButtons();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		if (intent.getBooleanExtra("stop_service", false)){
			// If it's a call from the notification, stop the service.
			stopSelf();
		}else{
			// Make the service run in foreground so that the system does not shut it down.
			Intent notificationIntent = new Intent(this, PlayerService.class);
			notificationIntent.putExtra("stop_service", true);
			PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
			Notification notification = new Notification(
					R.drawable.ic_launcher, 
					"Spotify tray launched",
			        System.currentTimeMillis());
			notification.setLatestEventInfo(
					this, 
					"Spotify tray",
			        "Tap to close the widget.", 
			        pendingIntent);
			startForeground(86, notification);
		}
		return START_STICKY;
	}

	// The app is closing.
	@Override
	public void onDestroy() {
		if (mRootLayout != null)
			mWindowManager.removeView(mRootLayout);
	}

	// Drags the tray as per touch info
	private void dragTray(int action, int x, int y){
		switch (action){
		case MotionEvent.ACTION_DOWN:
			
			// Cancel any currently running animations/automatic tray movements.
			if (mTrayTimerTask!=null){
				mTrayTimerTask.cancel();
				mTrayAnimationTimer.cancel();
			}
			
			// Store the start points
			mStartDragX = x;
			//mStartDragY = y;
			mPrevDragX = x;
			mPrevDragY = y;
			break;
			
		case MotionEvent.ACTION_MOVE:
			
			// Calculate position of the whole tray according to the drag, and update layout.
			float deltaX = x-mPrevDragX;
			float deltaY = y-mPrevDragY;
			mRootLayoutParams.x += deltaX;
			mRootLayoutParams.y += deltaY;
			mPrevDragX = x;
			mPrevDragY = y;
			animateButtons();
			mWindowManager.updateViewLayout(mRootLayout, mRootLayoutParams);
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			
			// When the tray is released, bring it back to "open" or "closed" state.
			if ((mIsTrayOpen && (x-mStartDragX)<=0) ||
				(!mIsTrayOpen && (x-mStartDragX)>=0))
				mIsTrayOpen = !mIsTrayOpen;
			
			mTrayTimerTask = new TrayAnimationTimerTask();
			mTrayAnimationTimer = new Timer();
			mTrayAnimationTimer.schedule(mTrayTimerTask, 0, ANIMATION_FRAME_RATE);
			break;
		}
	}

	// Listens to the touch events on the tray.
	private class TrayTouchListener implements OnTouchListener {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			final int action = event.getActionMasked();

			switch (action) {
			case MotionEvent.ACTION_DOWN: 
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				// Filter and redirect the events to dragTray()
				dragTray(action, (int)event.getRawX(), (int)event.getRawY());
				break;
			default:
				return false;
			}
			return true;

		}
	}
	
	// Timer for animation/automatic movement of the tray.
	private class TrayAnimationTimerTask extends TimerTask{
		
		// Ultimate destination coordinates toward which the tray will move
		int mDestX;
		int mDestY;
		
		public TrayAnimationTimerTask(){
			
			// Setup destination coordinates based on the tray state. 
			super();
			if (!mIsTrayOpen){
				mDestX = -mLogoLayout.getWidth();
			}else{
				mDestX = -mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION;
			}
			
			// Keep upper edge of the widget within the upper limit of screen
			int screenHeight = getResources().getDisplayMetrics().heightPixels;
			mDestY = Math.max(
					screenHeight/TRAY_MOVEMENT_REGION_FRACTION, 
					mRootLayoutParams.y);
			
			// Keep lower edge of the widget within the lower limit of screen
			mDestY = Math.min(
					((TRAY_MOVEMENT_REGION_FRACTION-1)*screenHeight)/TRAY_MOVEMENT_REGION_FRACTION - mRootLayout.getWidth(), 
					mDestY);
		}
		
		// This function is called after every frame.
		@Override
		public void run() {
			
			// handler is used to run the function on main UI thread in order to
			// access the layouts and UI elements.
			mAnimationHandler.post(new Runnable() {
				@Override
				public void run() {
					
					// Update coordinates of the tray
					mRootLayoutParams.x = (2*(mRootLayoutParams.x-mDestX))/3 + mDestX;
					mRootLayoutParams.y = (2*(mRootLayoutParams.y-mDestY))/3 + mDestY;
					mWindowManager.updateViewLayout(mRootLayout, mRootLayoutParams);
					animateButtons();
					
					// Cancel animation when the destination is reached
					if (Math.abs(mRootLayoutParams.x-mDestX)<2 && Math.abs(mRootLayoutParams.y-mDestY)<2){
						TrayAnimationTimerTask.this.cancel();
						mTrayAnimationTimer.cancel();
					}
				}
			});
		}
	}
	
	// This function animates the buttons based on the position of the tray.
	private void animateButtons(){
		
		// Animate only if the tray is between open and close state.
		if (mRootLayoutParams.x < -mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION){
			
			// Scale the distance between open and close states to 0-1. 
			float relativeDistance = (mRootLayoutParams.x + mLogoLayout.getWidth())/(float)
					(-mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION + mLogoLayout.getWidth());
			
			// Limit it to 0-1 if it goes beyond 0-1 for any reason.
			relativeDistance=Math.max(relativeDistance, 0);
			relativeDistance=Math.min(relativeDistance, 1);
			
			// Setup animations
			AnimationSet animations = new AnimationSet(true);
			animations.setFillAfter(true);
			Animation animationAlpha = new AlphaAnimation(
					relativeDistance, 
					relativeDistance);
			animations.addAnimation(animationAlpha);

			Animation animationScale = new ScaleAnimation(
					relativeDistance, 
					relativeDistance, 
					relativeDistance, 
					relativeDistance);
			animations.addAnimation(animationScale);
			
			// Play the animations
			mPlayerButtonsLayout.startAnimation(animations);
			mSongInfoLayout.startAnimation(animations);
			mAlbumCoverLayout.startAnimation(animationAlpha);
		}else{
			
			// Clear all animations if the tray is being dragged - that is, when it is beyond the
			// normal open state.
			mPlayerButtonsLayout.clearAnimation();
			mSongInfoLayout.clearAnimation();
			mAlbumCoverLayout.clearAnimation();
		}
	}
	
	// Populates the arraylist with mock album cover images.
	private void populateAlbumCoverImages(){
		mAlbumCoverImagePaths.add("adele.png");
		mAlbumCoverImagePaths.add("greenday.jpg");
		mAlbumCoverImagePaths.add("daftpunk.jpg");
		mAlbumCoverImagePaths.add("graffiti.jpg");
		mAlbumCoverImagePaths.add("akon.jpg");
	}
	
	// Play next song
	public void nextButtonClicked(View view){
		mAlbumCoverImageIndex++;
		mAlbumCoverImageIndex%=mAlbumCoverImagePaths.size();
		switchSong(mAlbumCoverImageIndex);
	}
	
	// Play previous song
	public void prevButtonClicked(View view){
		mAlbumCoverImageIndex--;
		mAlbumCoverImageIndex+=mAlbumCoverImagePaths.size();
		mAlbumCoverImageIndex%=mAlbumCoverImagePaths.size();
		switchSong(mAlbumCoverImageIndex);
	}
	
	// Load new album cover image
	private void switchSong(int songIndex){
		
		InputStream is=null;
		try {
			is = getAssets().open(mAlbumCoverImagePaths.get(songIndex));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Load new bitmap
		Bitmap bmap = Utils.loadMaskedBitmap(is, mAlbumCoverLayout.getHeight(), mAlbumCoverLayout.getWidth());
		
		// Change backgrounds
		mAlbumCoverHelperLayout.setBackgroundDrawable(mAlbumCoverLayout.getBackground());
		mAlbumCoverLayout.setBackgroundDrawable(new BitmapDrawable(getResources(), bmap));
		
		// Animate the two layouts in order to achieve the fade in/fade out effect.
		// First normalise the distance between open and closed states (0-1)
		float relativeDistance = (mRootLayoutParams.x + mLogoLayout.getWidth())/(float)
				(-mRootLayout.getWidth()/TRAY_HIDDEN_FRACTION + mLogoLayout.getWidth());
		relativeDistance=Math.max(relativeDistance, 0);
		relativeDistance=Math.min(relativeDistance, 1);
		
		// Then use it to set final alpha in the animation.
		Animation fadeOutAnim = new AlphaAnimation(relativeDistance,0.f);
		fadeOutAnim.setFillAfter(true);
		fadeOutAnim.setDuration(1000);
		Animation fadeInAnim = new AlphaAnimation(0.f,relativeDistance);
		fadeInAnim.setFillAfter(true);
		fadeInAnim.setDuration(1000);
		mAlbumCoverHelperLayout.startAnimation(fadeOutAnim);
		mAlbumCoverLayout.startAnimation(fadeInAnim);
	}
}
