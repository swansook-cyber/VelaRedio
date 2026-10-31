package net.velalab.radio

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
   LaunchedEffect(refresh) {
    try {
     stations = withContext(Dispatchers.IO) { loadStations() }
     message = "พบ ${stations.size} สถานีประเทศไทย (ยังไม่คัดเฉพาะเพลงไทย)"
    } catch (e: Exception) { message = "โหลดไม่สำเร็จ: ${e.message}" }
   }
   MaterialTheme {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
     Text("Vela Radio", style = MaterialTheme.typography.headlineMedium)
     Text(message)
     OutlinedTextField(query, { query = it }, label = { Text("ค้นหาสถานี") }, modifier = Modifier.fillMaxWidth())
     Row {
      Button(onClick = { refresh++ }) { Text("รีเฟรช") }
      FilterChip(selected = favOnly, onClick = { favOnly = !favOnly }, label = { Text("โปรด") })
      Button(onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } }, enabled = ready.value) { Text("เล่น/หยุด") }
     }
     LazyColumn {
      items(stations.filter { it.name.contains(query, true) && (!favOnly || it.id in favorites) }, key = { it.id }) { s ->
       ListItem(headlineContent = { Text(s.name) }, trailingContent = {
        Row {
         TextButton(onClick = {
          favorites = favorites.toMutableSet().apply { if (!add(s.id)) remove(s.id) }.toSet()
          prefs.edit().putStringSet("favorites", favorites).apply()
         }) { Text(if (s.id in favorites) "★" else "☆") }
         Button(onClick = {
          controller?.let {
           val item = MediaItem.Builder().setMediaId(s.id).setUri(s.stream)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).build()).build()
           it.setMediaItem(item); it.prepare(); it.play()
           prefs.edit().putString("last_id", s.id).apply()
          }
         }, enabled = ready.value) { Text("ฟัง") }
        }
       })
       HorizontalDivider()
      }
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
