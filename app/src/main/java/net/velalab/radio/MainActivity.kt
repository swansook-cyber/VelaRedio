package net.velalab.radio

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private data class Station(val id: String, val name: String, val stream: String)
class MainActivity : ComponentActivity() {
 private var controllerFuture: ListenableFuture<MediaController>? = null
 private var controller: MediaController? = null
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs = getSharedPreferences("radio", MODE_PRIVATE)
  val token = SessionToken(this, ComponentName(this, RadioService::class.java))
  val ready = mutableStateOf(false)
  controllerFuture = MediaController.Builder(this, token).buildAsync().also { f ->
   f.addListener({
    try { controller = f.get(); ready.value = true }
    catch (_: Exception) { ready.value = false }
   }, ContextCompat.getMainExecutor(this))
  }
  setContent {
   var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
   var message by remember { mutableStateOf("กำลังโหลดสถานี") }
   var query by remember { mutableStateOf("") }
   var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
   var favOnly by remember { mutableStateOf(false) }
   var refresh by remember { mutableIntStateOf(0) }
   var nowPlaying by remember { mutableStateOf("ยังไม่ได้เลือกสถานี") }
   var playing by remember { mutableStateOf(false) }
   val wide = LocalConfiguration.current.screenWidthDp >= 700
   LaunchedEffect(refresh) {
    try {
     stations = withContext(Dispatchers.IO) { loadStations() }
     message = "พบ ${stations.size} สถานีประเทศไทย (ยังไม่คัดเฉพาะเพลงไทย)"
    } catch (e: Exception) { message = "โหลดไม่สำเร็จ: ${e.message}" }
   }
   MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF91B6FF), background = Color(0xFF0B1220), surface = Color(0xFF182438))) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220)).padding(if (wide) 28.dp else 16.dp)) {
     Text("Vela Radio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
     Text(message, color = Color(0xFFA7B8D2))
     Spacer(Modifier.height(12.dp))
     OutlinedTextField(query, { query = it }, label = { Text("ค้นหาสถานี") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
     Spacer(Modifier.height(8.dp))
     Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      FocusButton("รีเฟรช", { refresh++ })
      FocusButton(if (favOnly) "★ รายการโปรด" else "☆ รายการโปรด", { favOnly = !favOnly })
     }
     Spacer(Modifier.height(12.dp))
     Text("สถานีวิทยุ", style = MaterialTheme.typography.titleLarge)
     Spacer(Modifier.height(8.dp))
     val filtered = stations.filter { it.name.contains(query, true) && (!favOnly || it.id in favorites) }
     LazyVerticalGrid(
      columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (wide) 3 else 1),
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
     ) {
      items(filtered.size) { i ->
       val s = filtered[i]
       var focused by remember { mutableStateOf(false) }
       Column(Modifier
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFF91B6FF) else Color(0xFF33465F), RoundedCornerShape(16.dp))
        .background(if (focused) Color(0xFF263E60) else Color(0xFF182438), RoundedCornerShape(16.dp))
        .padding(14.dp)) {
        Text(s.name, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         FocusButton("ฟัง", {
          controller?.let {
           val item = MediaItem.Builder().setMediaId(s.id).setUri(s.stream)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).build()).build()
           it.setMediaItem(item); it.prepare(); it.play()
           nowPlaying = s.name; playing = true
           prefs.edit().putString("last_id", s.id).apply()
          }
         }, enabled = ready.value)
         FocusButton(if (s.id in favorites) "★" else "☆", {
          favorites = favorites.toMutableSet().apply { if (!add(s.id)) remove(s.id) }.toSet()
          prefs.edit().putStringSet("favorites", favorites).apply()
         })
        }
       }
      }
     }
     Spacer(Modifier.height(12.dp))
     Row(Modifier.fillMaxWidth().background(Color(0xFF182438), RoundedCornerShape(16.dp)).padding(14.dp),
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(Modifier.weight(1f)) {
       Text("NOW PLAYING", color = Color(0xFF91B6FF), style = MaterialTheme.typography.labelMedium)
       Text(nowPlaying, maxLines = 1, fontWeight = FontWeight.Bold)
      }
      FocusButton(if (playing) "หยุด" else "เล่น", {
       controller?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.play(); playing = true } }
      }, enabled = ready.value)
     }
    }
   }
  }
 }
 override fun onDestroy() {
  controllerFuture?.let { MediaController.releaseFuture(it) }
  controllerFuture = null
  controller = null
  super.onDestroy()
 }
}
private fun loadStations(): List<Station> {
 val url = URL("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/TH?limit=100&order=clickcount&reverse=true&hidebroken=true&is_https=true")
 val c = url.openConnection() as HttpURLConnection
 c.connectTimeout = 10000; c.readTimeout = 10000
 try {
  if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
  val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
  return (0 until arr.length()).mapNotNull { i ->
   val o = arr.getJSONObject(i)
   val stream = o.optString("url_resolved").ifBlank { o.optString("url") }
   val id = o.optString("stationuuid"); val name = o.optString("name")
   if (id.isBlank() || name.isBlank() || !stream.startsWith("https://")) null else Station(id, name, stream)
  }.distinctBy { it.id }
 } finally { c.disconnect() }
}

@Composable
private fun FocusButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
 var focused by remember { mutableStateOf(false) }
 Button(
  onClick = onClick,
  enabled = enabled,
  modifier = Modifier.onFocusChanged { focused = it.isFocused }
   .border(if (focused) 3.dp else 0.dp, Color.White, RoundedCornerShape(24.dp)),
  colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color(0xFF487DE8) else Color(0xFF355AAB))
 ) { Text(label) }
}
