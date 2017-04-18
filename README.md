SpotifyTray-Android
===================
This Android project provides simplistic code to produce a floating widget like Facebook's chatheads app. It is supported by Android 2.3.3+ (API 10+).

Please note that it does not provide or include any Spotify music streaming features and is essentially just the UI for demonstration purposes only. Also note that this is not a project developed/endorsed by Spotify.

## Features
Assume that it's a small muted music player without any audio.
- Tray can be dragged around the screen.
- Tray, when released, comes back to a specific region on y-axis.
- The tray can be tapped to open/close.
- The widgets on the tray animate when the tray is between close and open states.
- Next and previous music buttons change the current song and hence the album cover changes too. The change is animated.
- Play and pause buttons work, but the change is not obvious since there is no audio.
- Each song has a duration. After the song has played for that duration, it switches to next song which is evident from the animated change that occurs when next button is pressed. You will notice this if you stay on the first song for 30s.
- The widget can be shut down from notification menu.

## Demo
https://www.youtube.com/watch?v=hdxdTkdNkVs

## License
MIT License. Copyright 2014 Fouad Yaseen.
