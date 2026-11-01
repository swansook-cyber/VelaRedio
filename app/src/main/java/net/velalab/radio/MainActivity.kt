package net.velalab.radio

import android.content.ComponentName
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import kotlinx.coroutines.launch
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

private data class Station(val id: String, val name: String, val stream: String, val logo: String, val tags: String)
class MainActivity : ComponentActivity() {
 private var controllerFuture: ListenableFuture<MediaController>? = null
 private var controller: MediaController? = null
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  @Suppress("DEPRECATION")
  val installedVersionName = requireNotNull(packageManager.getPackageInfo(packageName, 0).versionName)
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
   var refresh by remember { mutableIntStateOf(0) }
   var nowPlaying by remember { mutableStateOf("ยังไม่ได้เลือกสถานี") }
   var playing by remember { mutableStateOf(false) }
   var searchOpen by remember { mutableStateOf(false) }
   var selectedId by remember { mutableStateOf(prefs.getString("last_id", "") ?: "") }
   var category by remember { mutableStateOf("ทั้งหมด") }
   val tv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
   val focusManager = LocalFocusManager.current
   val screenWidth = LocalConfiguration.current.screenWidthDp
   val wide = screenWidth >= 700
   val carLandscape = wide && !tv
   val sidebarWidth = if (carLandscape) 204.dp else 190.dp
   val gridState = rememberLazyGridState()
   val scope = rememberCoroutineScope()
   LaunchedEffect(refresh) {
    try {
     stations = withContext(Dispatchers.IO) { loadStations() }
     message = ""
    } catch (e: Exception) { message = "โหลดไม่สำเร็จ: ${e.message}" }
   }
   MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF91B6FF), background = Color(0xFF0B1220), surface = Color(0xFF182438))) {
    Column(Modifier.fillMaxSize().background(Color(0xFF101C34)).padding(if (wide) 28.dp else 16.dp)) {
     Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("◉ Vela Radio  •  LIVE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
      FocusButton("ค้นหา", { searchOpen = !searchOpen; if (!searchOpen) focusManager.clearFocus() })
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
     if (message.isNotBlank()) Text(message, color = Color(0xFFA7B8D2), style = MaterialTheme.typography.bodySmall)
     Spacer(Modifier.height(4.dp))
     val categories = listOf("ทั้งหมด", "รายการโปรด", "ล่าสุด", "ข่าว / พูดคุย", "เพลง", "ไม่ระบุประเภท")
     val filtered = stations.filter {
      it.name.contains(query, true) &&
       (category != "รายการโปรด" || it.id in favorites) &&
       (category != "ล่าสุด" || it.id == selectedId) &&
       (category in listOf("ทั้งหมด", "รายการโปรด", "ล่าสุด") || stationCategory(it.tags) == category)
     }
     Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
     if (wide) {
      Column(Modifier.width(sidebarWidth).fillMaxHeight().background(Color(0xFF111F35), RoundedCornerShape(18.dp)).padding(10.dp)) {
       Text("สถานี", color = Color.White, fontWeight = FontWeight.Bold)
       Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (carLandscape) 7.dp else 8.dp)) {
        categories.forEach { entry ->
         FocusButton(entry, { category = entry; scope.launch { gridState.scrollToItem(0) } },
          compact = true, selected = category == entry, touchMode = carLandscape)
        }
       }
       Spacer(Modifier.height(12.dp))
       Text("Vela Radio • By VelaLab", color = Color(0xFF8DA4C7), style = MaterialTheme.typography.labelSmall)
       Text("V$installedVersionName", modifier = Modifier.testTag("installed-version"), color = Color(0xFF68738D), style = MaterialTheme.typography.labelSmall)
      }
     }
     LazyVerticalGrid(
      columns = GridCells.Fixed(if (wide) 3 else if (screenWidth >= 480) 2 else 1),
      modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(),
      state = gridState,
      flingBehavior = rememberSnapFlingBehavior(lazyGridState = gridState),
      contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
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
         .height(if (carLandscape) 112.dp else if (wide) 112.dp else 128.dp)
         .padding(12.dp)
       ) {
        Row(verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.spacedBy(12.dp)) {
         val logoSize = if (carLandscape) 54.dp else 48.dp
         if (s.logo.startsWith("https://", ignoreCase = true)) {
          SubcomposeAsyncImage(
           model = s.logo,
           contentDescription = "โลโก้ " + s.name,
           modifier = Modifier.size(logoSize),
           contentScale = ContentScale.Fit
          ) {
           when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> RadioPlaceholder(Modifier.fillMaxSize())
           }
          }
         } else {
          RadioPlaceholder(Modifier.size(logoSize))
         }
         Text(s.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
          maxLines = 2, minLines = 2, color = Color.White, style = if (carLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
         Text(if (s.id in favorites) "★" else "☆", color = Color.White,
          style = MaterialTheme.typography.titleLarge,
          modifier = if (carLandscape) Modifier.clickable { toggleFavorite() }.padding(4.dp) else Modifier)
        }

       }
      }
     }
     }
     Spacer(Modifier.height(6.dp))
     
     Row(Modifier.fillMaxWidth().height(54.dp).background(Color(0xFF182438), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(Modifier.weight(1f)) {
       Text("NOW PLAYING", color = Color(0xFF91B6FF), style = MaterialTheme.typography.labelMedium)
       Text(nowPlaying, maxLines = 1, fontWeight = FontWeight.Bold, color = Color.White, style = if (carLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
      }
      FocusButton(if (playing) "หยุด" else "เล่น", {
       controller?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.play(); playing = true } }
      }, enabled = ready.value && selectedId.isNotBlank())
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
   if (id.isBlank() || name.isBlank() || !stream.startsWith("https://")) null else Station(id, name, stream, o.optString("favicon"), o.optString("tags"))
  }.distinctBy { it.id }
 } finally { c.disconnect() }
}

@Composable
private fun FocusButton(
 label: String, onClick: () -> Unit, enabled: Boolean = true,
 compact: Boolean = false, selected: Boolean = false, touchMode: Boolean = false
) {
 var focused by remember { mutableStateOf(false) }
 Button(
  onClick = onClick,
  enabled = enabled,
  contentPadding = if (compact) PaddingValues(horizontal = 10.dp, vertical = 2.dp)
   else ButtonDefaults.ContentPadding,
  modifier = Modifier
   .then(if (compact) Modifier.fillMaxWidth().height(if (touchMode) 44.dp else 38.dp) else Modifier.heightIn(min = 44.dp))
   .onFocusChanged { focused = it.isFocused }
   .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(24.dp)),
  shape = RoundedCornerShape(24.dp),
  colors = ButtonDefaults.buttonColors(
   containerColor = if (focused) Color(0xFF487DE8)
    else if (selected) Color(0xFF2455B5) else Color(0xFF264A92),
   contentColor = Color.White)
 ) {
  Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis,
   style = if (compact && touchMode) MaterialTheme.typography.titleLarge else if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge)
 }
}

private fun stationCategory(rawTags: String): String {
 val tags = rawTags.lowercase().split(",").map { it.trim() }.filter { it.isNotBlank() }
 if (tags.isEmpty()) return "ไม่ระบุประเภท"
 if (tags.any { it.contains("news") || it.contains("talk") || it.contains("ข่าว") }) return "ข่าว / พูดคุย"
 return "เพลง"
}

@Composable
private fun RadioPlaceholder(modifier: Modifier = Modifier) {
 Canvas(modifier) {
  val w = size.width; val h = size.height
  val ink = Color.White
  drawLine(ink, Offset(w*.22f,h*.32f), Offset(w*.67f,h*.12f), strokeWidth = w*.055f)
  drawRoundRect(ink, topLeft = Offset(w*.1f,h*.3f), size = androidx.compose.ui.geometry.Size(w*.8f,h*.58f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*.11f), style = Stroke(w*.055f))
  drawCircle(ink, radius = w*.13f, center = Offset(w*.32f,h*.59f), style = Stroke(w*.045f))
  for (i in 0..3) drawLine(ink, Offset(w*.55f,h*(.47f+i*.085f)), Offset(w*.78f,h*(.47f+i*.085f)), strokeWidth = w*.038f)
 }
}
