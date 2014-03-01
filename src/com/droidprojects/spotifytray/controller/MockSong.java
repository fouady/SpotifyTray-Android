package com.droidprojects.spotifytray.controller;

/**
 * Mock Song metadata
 * @author Fouad
 *
 */
public class MockSong {
	public String mAlbumCoverPath;
	public int mSongDuration;
	public int mCurrentPlayheadPosition;
	public String mTitle;
	public String mSinger;
	
	public MockSong(String singer, String title, String path, int songDuration){
		mTitle = title;
		mSinger = singer;
		mAlbumCoverPath = path;
		mSongDuration = songDuration;
		mCurrentPlayheadPosition = 0;
	}
}
