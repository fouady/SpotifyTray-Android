package com.droidprojects.spotifytray.controller;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;

/**
 * Mock playlist and playlist manager
 * @author Fouad
 *
 */
public class MockPlaylist {
	
	// Playlist state info
	private int mCurrentSongIndex = 0;
	private int mNumTotalSongs = 5;
	private ArrayList<MockSong> mPlaylistSongs = new ArrayList<MockSong>(mNumTotalSongs);
	private MockSong mCurrentSong;
	
	// Timers to imitate playback
	private Timer mMockSongPlayerTimer = new Timer();
	private TimerTask mMockSongPlayerTask = new MockSongPlayerTimerTask();
	
	// References to the UI class and threads
	private MockPlaylistListener mListener; 
	private Handler mListenerHandler;

	// Listener to be implemented by the observer UI class
	public interface MockPlaylistListener{
		public void updateSongProgress(int playheadPosition);
		public void startedNextSong();
		public Handler getHandler();
	}
	
	public MockPlaylist(MockPlaylistListener listener) {
		mListener = listener;
		mListenerHandler = mListener.getHandler();
		initializePlaylistInfo();
		mCurrentSong = mPlaylistSongs.get(mCurrentSongIndex);
	}
	
	private void initializePlaylistInfo(){
		mPlaylistSongs.add(new MockSong("Adele", "Rolling in the deep", "adele.png", 30));
		mPlaylistSongs.add(new MockSong("Unknown 1", "Some random song 1", "greenday.jpg", 70));
		mPlaylistSongs.add(new MockSong("Unknown 2", "Some random song 2", "daftpunk.jpg", 150));
		mPlaylistSongs.add(new MockSong("Unknown 3", "Some random song 3", "graffiti.jpg", 180));
		mPlaylistSongs.add(new MockSong("Akon", "Mr. Lonely", "akon.jpg", 200));
	}
	
	public void playNextSong(){
		stopCurrentSong();
		mCurrentSongIndex++;
		mCurrentSongIndex%=mNumTotalSongs;
		mCurrentSong = mPlaylistSongs.get(mCurrentSongIndex);
		playCurrentSong();
	}
	
	public void playPreviousSong(){
		stopCurrentSong();
		mCurrentSongIndex--;
		mCurrentSongIndex = (mCurrentSongIndex+mNumTotalSongs) % mNumTotalSongs;
		mCurrentSong = mPlaylistSongs.get(mCurrentSongIndex);
		playCurrentSong();
	}
	
	public void pauseCurrentSong(){
		mMockSongPlayerTask.cancel();
		mMockSongPlayerTimer.cancel();
	}
	
	public void stopCurrentSong(){
		mMockSongPlayerTask.cancel();
		mMockSongPlayerTimer.cancel();
		mCurrentSong.mCurrentPlayheadPosition=0;
	}
	
	public void playCurrentSong(){
		mMockSongPlayerTimer = new Timer();
		mMockSongPlayerTask = new MockSongPlayerTimerTask();
		
		mMockSongPlayerTimer.schedule(mMockSongPlayerTask, 0, 1000);
	}
	
	public MockSong getCurrentSongInfo(){
		return mCurrentSong;
	}
	
	private class MockSongPlayerTimerTask extends TimerTask{
		@Override
		public void run() {
			if (mCurrentSong.mCurrentPlayheadPosition < mCurrentSong.mSongDuration){
				mCurrentSong.mCurrentPlayheadPosition++;
				mListener.updateSongProgress(mCurrentSong.mCurrentPlayheadPosition);
			}else{
				playNextSong();
				mListenerHandler.post(new Runnable() {
					@Override
					public void run() {
						mListener.startedNextSong();
					}
				});
			}
		}
	}
}
