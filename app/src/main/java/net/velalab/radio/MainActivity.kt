package net.velalab.radio

import android.content.ComponentName
import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
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

private data class Station(val id: String, val name: String, val stream: String, val logo: String)
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
   var searchOpen by remember { mutableStateOf(false) }
   var selectedId by remember { mutableStateOf(prefs.getString("last_id", "") ?: "") }
   var category by remember { mutableStateOf("ทั้งหมด") }
   val tv = LocalConfiguration.current.screenWidthDp >= 700
   val focusManager = LocalFocusManager.current
   val wide = LocalConfiguration.current.screenWidthDp >= 700
   LaunchedEffect(refresh) {
    try {
     stations = withContext(Dispatchers.IO) { loadStations() }
     message = "พบ ${stations.size} สถานีประเทศไทย (ยังไม่คัดเฉพาะเพลงไทย)"
    } catch (e: Exception) { message = "โหลดไม่สำเร็จ: ${e.message}" }
   }
   MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF91B6FF), background = Color(0xFF0B1220), surface = Color(0xFF182438))) {
    Column(Modifier.fillMaxSize().background(Color(0xFF101C34)).padding(if (wide) 28.dp else 16.dp)) {
     Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("◉ Vela Radio  •  LIVE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
      FocusButton("ค้นหา", { searchOpen = !searchOpen; if (!searchOpen) focusManager.clearFocus() })
      FocusButton(if (favOnly) "★ โปรด" else "☆ โปรด", { favOnly = !favOnly })
      FocusButton("รีเฟรช", { refresh++ })
     }
     if (searchOpen) {
      OutlinedTextField(query, { query = it }, label = { Text("ค้นหาสถานี") },
       modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyUp && it.key == Key.Back) {
         searchOpen = false; focusManager.clearFocus(); true
        } else false
       }, singleLine = true)
     }
     Text(message, color = Color(0xFFA7B8D2), style = MaterialTheme.typography.bodySmall)
     Spacer(Modifier.height(8.dp))
     val filtered = stations.filter { it.name.contains(query, true) && (!favOnly || it.id in favorites) && (category != "ล่าสุด" || it.id == selectedId) }
     Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
     if (tv) {
      Column(Modifier.width(170.dp).fillMaxHeight().background(Color(0xFF111F35), RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
       Text("สถานี", color = Color.White, fontWeight = FontWeight.Bold)
       listOf("ทั้งหมด", "รายการโปรด", "ล่าสุด").forEach { entry ->
        FocusButton(entry, { category = entry; favOnly = entry == "รายการโปรด" })
       }
       Spacer(Modifier.weight(1f))
       Text("VelaLab • Radio", color = Color(0xFF91B6FF), style = MaterialTheme.typography.labelSmall)
      }
     }
     LazyVerticalGrid(
      columns = GridCells.Fixed(if (wide) 3 else 1),
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
     ) {
      items(filtered.size) { i ->
       val s = filtered[i]
       var focused by remember(s.id) { mutableStateOf(false) }
       var longPressHandled by remember(s.id) { mutableStateOf(false) }
       val toggleFavorite = {
        favorites = favorites.toMutableSet().apply { if (!add(s.id)) remove(s.id) }.toSet()
        prefs.edit().putStringSet("favorites", favorites).apply()
       }
       Column(
        Modifier
         .onPreviewKeyEvent { event ->
          val native = event.nativeKeyEvent
          val select = native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
           native.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
           native.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
          if (!select) false
          else if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount >= 1) {
           if (!longPressHandled) { toggleFavorite(); longPressHandled = true }
           true
          } else if (native.action == AndroidKeyEvent.ACTION_UP && longPressHandled) {
           longPressHandled = false
           true
          } else false
         }
         .onFocusChanged { focused = it.isFocused }
         .border(if (focused) 3.dp else 1.dp,
          if (focused) Color.White else Color(0xFF405473), RoundedCornerShape(16.dp))
         .background(
          if (focused) Color(0xFF3B599E)
          else listOf(Color(0xFF164F80), Color(0xFF673C93), Color(0xFF9B3769),
           Color(0xFF23766F), Color(0xFF985B27))[i % 5],
          RoundedCornerShape(16.dp))
         .clickable(enabled = ready.value) {
          controller?.let { p ->
           p.setMediaItem(MediaItem.Builder().setMediaId(s.id).setUri(s.stream)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).build()).build())
           p.prepare(); p.play()
           nowPlaying = s.name; selectedId = s.id; playing = true
           prefs.edit().putString("last_id", s.id).apply()
          }
         }
         .padding(14.dp)
       ) {
        Row(verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.spacedBy(12.dp)) {
         if (s.logo.startsWith("https://")) {
          AsyncImage(model = s.logo, contentDescription = "โลโก้ " + s.name,
           modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
         } else {
          Text("♫", color = Color.White, style = MaterialTheme.typography.headlineLarge)
         }
         Text(s.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
          maxLines = 2, minLines = 2, color = Color.White)
         Text(if (s.id in favorites) "★" else "☆", color = Color.White,
          style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(10.dp))
        Text("OK ฟัง  •  OK ค้าง เพิ่ม/ลบโปรด",
         color = Color(0xFFE2E9FF), style = MaterialTheme.typography.labelSmall)
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
       Text(nowPlaying, maxLines = 1, fontWeight = FontWeight.Bold, color = Color.White)
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
   if (id.isBlank() || name.isBlank() || !stream.startsWith("https://")) null else Station(id, name, stream, o.optString("favicon"))
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
  colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color(0xFF487DE8) else Color(0xFF3F6BD6), contentColor = Color.White)
 ) { Text(label) }
}
