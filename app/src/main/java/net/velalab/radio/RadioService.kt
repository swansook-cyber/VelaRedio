package net.velalab.radio

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioService : MediaSessionService() {
 private var player: ExoPlayer? = null
 private var session: MediaSession? = null
 override fun onCreate() {
  super.onCreate()
  val p = ExoPlayer.Builder(this).build()
  player = p
  session = MediaSession.Builder(this, p).build()
 }
 override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
 override fun onDestroy() {
  session?.release()
  player?.release()
  session = null
  player = null
  super.onDestroy()
 }
}
