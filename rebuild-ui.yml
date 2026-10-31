name: Rebuild UI + Verify

on:
  workflow_dispatch:

jobs:
  fix:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
      - name: Write files
        run: |
          mkdir -p core-design/src/main/java/com/linkdeck/core/design
          cat > core-design/src/main/java/com/linkdeck/core/design/Type.kt <<'KOTLIN_EOF'
          package com.linkdeck.core.design
          import android.content.Context
          import android.graphics.Typeface
          import android.os.Build
          import androidx.compose.material3.Typography
          import androidx.compose.ui.text.PlatformTextStyle
          import androidx.compose.ui.text.TextStyle
          import androidx.compose.ui.text.font.FontWeight
          import androidx.compose.ui.unit.sp
          enum class AppFont(val fileName: String) {
              CAIRO("cairo.ttf"),
              IBM_PLEX("ibm.ttf"),
              MADIKA("madika.ttf"),
              BASSEET("basseet.ttf"),
              CAMEL("camel.ttf"),
              SYSTEM("");
          }
          enum class IconStroke(val width: Float) {
              THIN(1.5f), REGULAR(2f), BOLD(2.75f);
          }
          @Suppress("DEPRECATION")
          fun loadAppTypeface(context: Context, font: AppFont, weight: Int?): Typeface? {
              if (font == AppFont.SYSTEM) return null
              val base: Typeface = runCatching {
                  if (Build.VERSION.SDK_INT >= 26) {
                      Typeface.Builder(context.assets, "fonts/${font.fileName}").build()
                  } else {
                      Typeface.createFromAsset(context.assets, "fonts/${font.fileName}")
                  }
              }.getOrNull() ?: return null
              return if (weight != null && Build.VERSION.SDK_INT >= 28) {
                  Typeface.create(base, weight, false)
              } else base
          }
          @Suppress("DEPRECATION")
          fun linkDeckTypography(typeface: Typeface?, fontScale: Float, weight: Int?): Typography {
              val platformStyle = typeface?.let { PlatformTextStyle(includeFontPadding = null, typeface = it) }
              val w = FontWeight(weight ?: 500)
              fun s(sp: Int) = (sp * fontScale).sp
              fun style(sizeSp: Int) = TextStyle(
                  fontSize = s(sizeSp),
                  fontWeight = w,
                  platformStyle = platformStyle,
              )
              return Typography(
                  headlineMedium = style(22),
                  headlineSmall = style(20),
                  titleLarge = style(22),
                  titleMedium = style(15),
                  titleSmall = style(13),
                  bodyLarge = style(15),
                  bodyMedium = style(14),
                  bodySmall = style(12),
                  labelLarge = style(14),
                  labelMedium = style(12),
                  labelSmall = style(11),
              )
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/settings
          cat > app/src/main/java/com/linkdeck/app/ui/settings/SettingsScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.settings
          import android.content.ComponentName
          import android.content.pm.PackageManager
          import androidx.activity.compose.rememberLauncherForActivityResult
          import androidx.compose.foundation.layout.Arrangement
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Row
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.height
          import androidx.compose.foundation.layout.padding
          import androidx.compose.foundation.rememberScrollState
          import androidx.compose.foundation.shape.RoundedCornerShape
          import androidx.compose.foundation.verticalScroll
          import androidx.compose.material3.AlertDialog
          import androidx.compose.material3.ExperimentalMaterial3Api
          import androidx.compose.material3.IconButton
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.OutlinedButton
          import androidx.compose.material3.Scaffold
          import androidx.compose.material3.SegmentedButton
          import androidx.compose.material3.SegmentedButtonDefaults
          import androidx.compose.material3.SingleChoiceSegmentedButtonRow
          import androidx.compose.material3.Slider
          import androidx.compose.material3.Surface
          import androidx.compose.material3.Text
          import androidx.compose.material3.TextButton
          import androidx.compose.material3.TopAppBar
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.collectAsState
          import androidx.compose.runtime.getValue
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.remember
          import androidx.compose.runtime.setValue
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.platform.LocalContext
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.unit.dp
          import androidx.hilt.navigation.compose.hiltViewModel
          import com.linkdeck.app.MainViewModel
          import com.linkdeck.app.R
          import com.linkdeck.app.settings.AppLanguage
          import com.linkdeck.app.settings.IconStyle
          import com.linkdeck.core.design.AccentTheme
          import com.linkdeck.core.design.AppFont
          import com.linkdeck.core.design.IconStroke
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.LinkDeckTheme
          import com.linkdeck.core.design.Spacing
          import com.linkdeck.core.design.ThemeMode
          private val ICON_ALIASES = mapOf(
              IconStyle.DEFAULT to "com.linkdeck.app.alias.IconDefault",
              IconStyle.TEAL to "com.linkdeck.app.alias.IconTeal",
              IconStyle.INDIGO to "com.linkdeck.app.alias.IconIndigo",
              IconStyle.DARK to "com.linkdeck.app.alias.IconDark",
          )
          @OptIn(ExperimentalMaterial3Api::class)
          @Composable
          fun SettingsScreen(onBack: () -> Unit, onOpenGuide: () -> Unit = {}, onOpenCustomize: () -> Unit = {}) {
              val vm: MainViewModel = hiltViewModel()
              val context = LocalContext.current
              var showHideWarning by remember { mutableStateOf(false) }
              val themeMode by vm.themeMode.collectAsState()
              val accent by vm.accent.collectAsState()
              val language by vm.language.collectAsState()
              val font by vm.fontFamily.collectAsState()
              val fontScale by vm.fontScale.collectAsState()
              val fontWeight by vm.fontWeight.collectAsState()
              val cornerScale by vm.cornerScale.collectAsState()
              val iconStroke by vm.iconStroke.collectAsState()
              val iconStyle by vm.settings.iconStyle.collectAsState()
              fun applyIconStyle(style: IconStyle) {
                  val pm = context.packageManager
                  ICON_ALIASES.forEach { (s, name) ->
                      val state = if (style == s) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                      pm.setComponentEnabledSetting(
                          ComponentName(context.packageName, name), state, PackageManager.DONT_KILL_APP,
                      )
                  }
                  vm.settings.setIconStyle(style)
              }
              Scaffold(
                  topBar = {
                      TopAppBar(
                          title = { Text(stringResource(R.string.settings)) },
                          navigationIcon = {
                              IconButton(onClick = onBack) {
                                  LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                              }
                          },
                      )
                  },
              ) { padding ->
                  Column(
                      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.xl),
                      verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                  ) {
                      SectionTitle(stringResource(R.string.settings_theme))
                      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                          ThemeMode.entries.forEachIndexed { i, m ->
                              SegmentedButton(
                                  selected = themeMode == m,
                                  onClick = { vm.settings.setThemeMode(m) },
                                  shape = SegmentedButtonDefaults.itemShape(index = i, count = ThemeMode.entries.size),
                              ) { Text(stringResource(when (m) {
                                  ThemeMode.SYSTEM -> R.string.theme_system
                                  ThemeMode.LIGHT -> R.string.theme_light
                                  ThemeMode.DARK -> R.string.theme_dark
                              })) }
                          }
                      }
                      SectionTitle(stringResource(R.string.settings_accent))
                      AccentTheme.entries.forEach { a ->
                          SelectRow(
                              label = stringResource(when (a) {
                                  AccentTheme.DEFAULT -> R.string.accent_default
                                  AccentTheme.TEAL -> R.string.accent_teal
                                  AccentTheme.INDIGO -> R.string.accent_indigo
                                  AccentTheme.AMBER -> R.string.accent_amber
                                  AccentTheme.ROSE -> R.string.accent_rose
                                  AccentTheme.DYNAMIC -> R.string.accent_dynamic
                              }),
                              selected = accent == a,
                              onClick = { vm.settings.setAccent(a) },
                          )
                      }
                      SectionTitle(stringResource(R.string.settings_language))
                      AppLanguage.entries.forEach { l ->
                          SelectRow(
                              label = stringResource(when (l) {
                                  AppLanguage.AUTO -> R.string.lang_auto
                                  AppLanguage.ARABIC -> R.string.lang_ar
                                  AppLanguage.ENGLISH -> R.string.lang_en
                              }),
                              selected = language == l,
                              onClick = { vm.settings.setLanguage(l) },
                          )
                      }
                      SectionTitle(stringResource(R.string.settings_font))
                      AppFont.entries.forEach { f ->
                          SelectRow(
                              label = stringResource(
                                  when (f) {
                                      AppFont.CAIRO -> R.string.font_cairo
                                      AppFont.IBM_PLEX -> R.string.font_ibm
                                      AppFont.MADIKA -> R.string.font_madika
                                      AppFont.BASSEET -> R.string.font_basseet
                                      AppFont.CAMEL -> R.string.font_camel
                                      AppFont.SYSTEM -> R.string.font_system
                                  }
                              ),
                              selected = font == f,
                              onClick = { vm.settings.setFontFamily(f) },
                          )
                      }
                      Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleMedium)
                      Slider(value = fontScale, onValueChange = { vm.settings.setFontScale(it) }, valueRange = 0.85f..1.3f)
                      Text(stringResource(R.string.settings_font_weight), style = MaterialTheme.typography.titleMedium)
                      Slider(value = fontWeight.toFloat(), onValueChange = { vm.settings.setFontWeight(it.toInt()) }, valueRange = 300f..800f)
                      SectionTitle(stringResource(R.string.settings_corners))
                      Slider(value = cornerScale, onValueChange = { vm.settings.setCornerScale(it) }, valueRange = 0.5f..1.5f)
                      SectionTitle(stringResource(R.string.settings_icon_stroke))
                      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                          IconStroke.entries.forEachIndexed { i, s ->
                              SegmentedButton(
                                  selected = iconStroke == s,
                                  onClick = { vm.settings.setIconStroke(s) },
                                  shape = SegmentedButtonDefaults.itemShape(index = i, count = IconStroke.entries.size),
                              ) { Text(stringResource(when (s) {
                                  IconStroke.THIN -> R.string.stroke_thin
                                  IconStroke.REGULAR -> R.string.stroke_regular
                                  IconStroke.BOLD -> R.string.stroke_bold
                              })) }
                          }
                      }
                      SectionTitle(stringResource(R.string.settings_icon_style))
                      IconStyle.entries.forEach { s ->
                          SelectRow(
                              label = stringResource(when (s) {
                                  IconStyle.DEFAULT -> R.string.icon_style_default
                                  IconStyle.TEAL -> R.string.icon_style_teal
                                  IconStyle.INDIGO -> R.string.icon_style_indigo
                                  IconStyle.DARK -> R.string.icon_style_dark
                                  IconStyle.HIDDEN -> R.string.icon_style_hidden
                              }),
                              selected = iconStyle == s,
                              onClick = { if (s == IconStyle.HIDDEN) showHideWarning = true else applyIconStyle(s) },
                          )
                      }
                      SelectRow(label = stringResource(R.string.nav_customize_title), selected = false, onClick = onOpenCustomize)
                      SelectRow(label = stringResource(R.string.guide_title), selected = false, onClick = onOpenGuide)
                      val themeJson = org.json.JSONObject().apply {
                          put("schema", 1); put("app", "LinkDeck"); put("type", ".bfbtheme")
                          put("theme", themeMode.name); put("accent", accent.name); put("font", font.name)
                          put("fontScale", fontScale.toDouble()); put("fontWeight", fontWeight)
                          put("corner", cornerScale.toDouble()); put("iconStroke", iconStroke.name)
                          put("iconStyle", iconStyle.name)
                      }.toString()
                      var themeMsg by remember { mutableStateOf<String?>(null) }
                      val exportLauncher = rememberLauncherForActivityResult(
                          androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
                      ) { uri ->
                          uri?.let { runCatching { context.contentResolver.openOutputStream(it)?.use { o -> o.write(themeJson.toByteArray()) } } }
                      }
                      val importLauncher = rememberLauncherForActivityResult(
                          androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                      ) { uri ->
                          themeMsg = runCatching {
                              val text = uri?.let { context.contentResolver.openInputStream(it)?.use { i -> i.readBytes().decodeToString() } } ?: error("empty")
                              if (text.length > 65536) error("file too large (max 64KB)")
                              val j = org.json.JSONObject(text)
                              if (j.optInt("schema") != 1 || j.optString("app") != "LinkDeck") error("invalid .bfbtheme file")
                              val pTheme = j.getString("theme"); val pAccent = j.getString("accent"); val pFont = j.getString("font")
                              val pFs = j.getDouble("fontScale").toFloat(); val pW = j.getInt("fontWeight")
                              val pC = j.getDouble("corner").toFloat(); val pS = j.getString("iconStroke")
                              themeMsg = "Preview: $pTheme/$pAccent/$pFont · ${pFs}x · w$pW"
                              vm.settings.setThemeMode(com.linkdeck.core.design.ThemeMode.valueOf(pTheme))
                              vm.settings.setAccent(com.linkdeck.core.design.AccentTheme.valueOf(pAccent))
                              vm.settings.setFontFamily(com.linkdeck.core.design.AppFont.valueOf(pFont))
                              vm.settings.setFontScale(pFs.coerceIn(0.85f, 1.3f))
                              vm.settings.setFontWeight(pW.coerceIn(300, 800))
                              vm.settings.setCornerScale(pC.coerceIn(0.5f, 1.5f))
                              vm.settings.setIconStroke(com.linkdeck.core.design.IconStroke.valueOf(pS))
                              "Theme applied ✓"
                          }.getOrElse { "Import failed: ${it.message}" }
                      }
                      OutlinedButton(onClick = { exportLauncher.launch("linkdeck.bfbtheme.json") }, modifier = Modifier.fillMaxWidth()) {
                          Text("Export .bfbtheme")
                      }
                      OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                          Text("Import .bfbtheme")
                      }
                      themeMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                      Spacer(Modifier.height(Spacing.sm))
                      OutlinedButton(
                          onClick = {
                              vm.settings.resetToDefaults()
                              applyIconStyle(IconStyle.DEFAULT)
                          },
                          modifier = Modifier.fillMaxWidth(),
                      ) { Text(stringResource(R.string.reset_default)) }
                  }
              }
              if (showHideWarning) {
                  AlertDialog(
                      onDismissRequest = { showHideWarning = false },
                      title = { Text(stringResource(R.string.hide_icon_warning_title)) },
                      text = { Text(stringResource(R.string.hide_icon_warning_body)) },
                      confirmButton = {
                          TextButton(onClick = {
                              val pm = context.packageManager
                              ICON_ALIASES.values.forEach { name ->
                                  pm.setComponentEnabledSetting(
                                      ComponentName(context.packageName, name),
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                                  )
                              }
                              vm.settings.setIconStyle(IconStyle.HIDDEN)
                              showHideWarning = false
                          }) { Text(stringResource(R.string.hide_icon_confirm), color = MaterialTheme.colorScheme.error) }
                      },
                      dismissButton = {
                          TextButton(onClick = { showHideWarning = false }) { Text(stringResource(R.string.cancel)) }
                      },
                  )
              }
          }
          @Composable
          private fun SectionTitle(text: String) {
              Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          }
          @Composable
          private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
              Surface(
                  color = if (selected) LinkDeckTheme.tints.accentBg else MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp),
                  onClick = onClick,
                  modifier = Modifier.fillMaxWidth(),
              ) {
                  Row(Modifier.padding(Spacing.xl)) {
                      Text(
                          label, style = MaterialTheme.typography.bodyLarge,
                          color = if (selected) LinkDeckTheme.tints.accentFg else MaterialTheme.colorScheme.onSurface,
                      )
                  }
              }
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/pairing
          cat > app/src/main/java/com/linkdeck/app/ui/pairing/ShowQrScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.pairing
          import androidx.compose.foundation.Image
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.height
          import androidx.compose.foundation.layout.padding
          import androidx.compose.foundation.layout.size
          import androidx.compose.foundation.shape.RoundedCornerShape
          import androidx.compose.material3.Button
          import androidx.compose.material3.CircularProgressIndicator
          import androidx.compose.material3.ExperimentalMaterial3Api
          import androidx.compose.material3.IconButton
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.Scaffold
          import androidx.compose.material3.Surface
          import androidx.compose.material3.Text
          import androidx.compose.material3.TopAppBar
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.collectAsState
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.remember
          import androidx.compose.runtime.setValue
          import androidx.compose.runtime.getValue
          import androidx.compose.ui.Alignment
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.graphics.Color
          import androidx.compose.ui.graphics.asImageBitmap
          import androidx.compose.ui.layout.ContentScale
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.unit.dp
          import androidx.hilt.navigation.compose.hiltViewModel
          import androidx.lifecycle.ViewModel
          import com.linkdeck.app.R
          import com.linkdeck.app.pairing.HostQrState
          import com.linkdeck.app.pairing.PairingManager
          import com.linkdeck.app.pairing.rememberQrBitmap
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.Spacing
          import dagger.hilt.android.lifecycle.HiltViewModel
          import javax.inject.Inject
          @HiltViewModel
          class ShowQrViewModel @Inject constructor(val pairingManager: PairingManager) : ViewModel()
          @OptIn(ExperimentalMaterial3Api::class)
          @Composable
          fun ShowQrScreen(onBack: () -> Unit) {
              val vm: ShowQrViewModel = hiltViewModel()
              val state by vm.pairingManager.hostQr.collectAsState()
              androidx.compose.runtime.LaunchedEffect(state.secondsLeft) {
                  if (state.secondsLeft <= 0) vm.pairingManager.rotateQrToken()
              }
              Scaffold(
                  topBar = {
                      TopAppBar(
                          title = { Text(stringResource(R.string.qr_title_host)) },
                          navigationIcon = {
                              IconButton(onClick = onBack) {
                                  LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                              }
                          },
                      )
                  },
              ) { padding ->
                  Column(
                      Modifier.fillMaxSize().padding(padding).padding(Spacing.xxl),
                      horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                      val payload = state.payload
                      if (payload == null) {
                          CircularProgressIndicator()
                          return@Column
                      }
                      val bitmap = rememberQrBitmap(payload.toQrContent())
                      Surface(
                          color = Color.White,
                          shape = RoundedCornerShape(16.dp),
                          modifier = Modifier.size(280.dp),
                      ) {
                          if (bitmap != null) {
                              Image(
                                  bitmap = bitmap,
                                  contentDescription = stringResource(R.string.qr_title_host),
                                  contentScale = ContentScale.Fit,
                                  modifier = Modifier.fillMaxSize().padding(12.dp),
                              )
                          }
                      }
                      Spacer(Modifier.height(Spacing.xl))
                      Text(
                          text = stringResource(R.string.qr_expires_in, state.secondsLeft),
                          style = MaterialTheme.typography.bodyMedium,
                          color = if (state.secondsLeft < 20) MaterialTheme.colorScheme.error
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Spacer(Modifier.height(Spacing.sm))
                      Text(
                          text = payload.ip,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Spacer(Modifier.height(Spacing.xs))
                      Text(
                          text = "build " + com.linkdeck.app.BuildConfig.VERSION_NAME + " (" + com.linkdeck.app.BuildConfig.BUILD_TYPE + ")",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.outline,
                      )
                      Spacer(Modifier.height(Spacing.xl))
                      Button(onClick = { vm.pairingManager.rotateQrToken() }) {
                          Text(stringResource(R.string.qr_regenerate))
                      }
                      Spacer(Modifier.height(Spacing.sm))
                      var showCode by remember { mutableStateOf(false) }
                      TextButton(onClick = { showCode = !showCode }) {
                          Text(stringResource(R.string.show_code_text))
                      }
                      if (showCode) {
                          androidx.compose.foundation.text.selection.SelectionContainer {
                              Text(
                                  payload.toQrContent(),
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier = Modifier.padding(horizontal = Spacing.xl),
                              )
                          }
                      }
                  }
              }
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/pairing
          cat > app/src/main/java/com/linkdeck/app/ui/pairing/ScanQrScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.pairing
          import android.Manifest
          import androidx.activity.compose.rememberLauncherForActivityResult
          import androidx.activity.result.contract.ActivityResultContracts
          import androidx.camera.core.CameraSelector
          import androidx.camera.core.ImageAnalysis
          import androidx.camera.core.Preview
          import androidx.camera.lifecycle.ProcessCameraProvider
          import androidx.camera.view.PreviewView
          import androidx.compose.foundation.layout.Arrangement
          import androidx.compose.foundation.layout.Box
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.height
          import androidx.compose.foundation.layout.padding
          import androidx.compose.material3.Button
          import androidx.compose.material3.ExperimentalMaterial3Api
          import androidx.compose.material3.IconButton
          import androidx.compose.material3.LinearProgressIndicator
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.OutlinedTextField
          import androidx.compose.material3.AlertDialog
          import androidx.compose.material3.Scaffold
          import androidx.compose.material3.Text
          import androidx.compose.material3.TopAppBar
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.LaunchedEffect
          import androidx.compose.runtime.collectAsState
          import androidx.compose.runtime.getValue
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.remember
          import androidx.compose.runtime.setValue
          import androidx.compose.ui.Alignment
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.platform.LocalClipboardManager
          import androidx.compose.ui.platform.LocalContext
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.text.AnnotatedString
          import androidx.compose.ui.unit.dp
          import androidx.core.content.ContextCompat
          import androidx.hilt.navigation.compose.hiltViewModel
          import androidx.lifecycle.ViewModel
          import androidx.lifecycle.viewModelScope
          import com.linkdeck.app.R
          import com.linkdeck.app.pairing.PairingManager
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.Spacing
          import com.linkdeck.core.network.PairingPayload
          import dagger.hilt.android.lifecycle.HiltViewModel
          import javax.inject.Inject
          import kotlinx.coroutines.flow.MutableStateFlow
          import kotlinx.coroutines.flow.StateFlow
          import kotlinx.coroutines.launch
          sealed interface ScanUiState {
              data object NeedsCameraPermission : ScanUiState
              data object Scanning : ScanUiState
              data object Pairing : ScanUiState
              data class Done(val deviceName: String) : ScanUiState
              data class Error(val message: String) : ScanUiState
          }
          @HiltViewModel
          class ScanQrViewModel @Inject constructor(
              val pairingManager: PairingManager,
              private val connectionManager: com.linkdeck.core.network.ConnectionManager,
          ) : ViewModel() {
              private val _state = MutableStateFlow<ScanUiState>(ScanUiState.NeedsCameraPermission)
              val state: StateFlow<ScanUiState> = _state
              fun onPermissionResult(granted: Boolean) {
                  _state.value = if (granted) ScanUiState.Scanning else ScanUiState.NeedsCameraPermission
              }
              fun onScanned(content: String) {
                  if (_state.value != ScanUiState.Scanning) return
                  if (!content.contains("\"token\"")) return
                  val payload = PairingPayload.fromQrContent(content)
                  if (payload == null) {
                      _state.value = ScanUiState.Error("scanned: ${content.take(120)}")
                      return
                  }
                  if (payload.isExpired()) {
                      _state.value = ScanUiState.Error("token_expired")
                      return
                  }
                  _state.value = ScanUiState.Pairing
                  viewModelScope.launch {
                      pairingManager.pairWithPayload(payload)
                          .onSuccess { device ->
                              _state.value = ScanUiState.Done(device.name)
                          }
                          .onFailure { e ->
                              _state.value = ScanUiState.Error(e.message ?: "pairing_failed")
                          }
                  }
              }
              fun retry() { _state.value = ScanUiState.Scanning }
              private val _hostLog = MutableStateFlow<String?>(null)
              val hostLog: StateFlow<String?> = _hostLog
              fun fetchHostLog() = viewModelScope.launch {
                  _hostLog.value = "loading…"
                  connectionManager.request(
                      "cmd",
                      com.linkdeck.core.network.ProtoJson.encodeToString(
                          com.linkdeck.core.network.CmdReq.serializer(),
                          com.linkdeck.core.network.CmdReq("get_log"),
                      ),
                  ).onSuccess { _hostLog.value = it }
                      .onFailure { _hostLog.value = "unavailable: ${'$'}{it.message}" }
              }
              fun pairOverP2p(ip: String, port: Int, code: String) {
                  _state.value = ScanUiState.Pairing
                  viewModelScope.launch {
                      pairingManager.pairOverP2p(ip, port, code)
                          .onSuccess { _state.value = ScanUiState.Done(it.name) }
                          .onFailure { _state.value = ScanUiState.Error(it.message ?: "pairing_failed") }
                  }
              }
              fun pairManual(ip: String, port: Int, token: String, fp: String) {
                  _state.value = ScanUiState.Pairing
                  viewModelScope.launch {
                      val payload = PairingPayload(
                          name = "Manual", model = "manual", ip = ip, port = port,
                          fp = fp, token = token, exp = System.currentTimeMillis() + 120_000L,
                      )
                      pairingManager.pairWithPayload(payload)
                          .onSuccess { _state.value = ScanUiState.Done(it.name) }
                          .onFailure { _state.value = ScanUiState.Error(it.message ?: "pairing_failed") }
                  }
              }
          }
          @OptIn(ExperimentalMaterial3Api::class)
          @Composable
          fun ScanQrScreen(onBack: () -> Unit) {
              val vm: ScanQrViewModel = hiltViewModel()
              val state by vm.state.collectAsState()
              val context = LocalContext.current
              val permissionLauncher = rememberLauncherForActivityResult(
                  ActivityResultContracts.RequestPermission(),
              ) { granted -> vm.onPermissionResult(granted) }
              LaunchedEffect(Unit) {
                  permissionLauncher.launch(Manifest.permission.CAMERA)
              }
              Scaffold(
                  topBar = {
                      TopAppBar(
                          title = { Text(stringResource(R.string.qr_title_scan)) },
                          navigationIcon = {
                              IconButton(onClick = onBack) {
                                  LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                              }
                          },
                      )
                  },
              ) { padding ->
                  Column(Modifier.fillMaxSize().padding(padding)) {
                      when (state) {
                          ScanUiState.NeedsCameraPermission -> {
                              Column(
                                  Modifier.fillMaxSize().padding(Spacing.xxl),
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  verticalArrangement = Arrangement.Center,
                              ) {
                                  Text(stringResource(R.string.perm_camera_why), style = MaterialTheme.typography.bodyMedium)
                                  Spacer(Modifier.height(Spacing.xl))
                                  Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                      Text(stringResource(R.string.perm_grant))
                                  }
                              }
                          }
                          ScanUiState.Scanning -> {
                              Text(
                                  stringResource(R.string.qr_scan_hint),
                                  style = MaterialTheme.typography.bodyMedium,
                                  modifier = Modifier.padding(Spacing.xl),
                              )
                              var showManual by remember { mutableStateOf(false) }
                              TextButton(onClick = { showManual = true }, modifier = Modifier.padding(horizontal = Spacing.xl)) {
                                  Text(stringResource(R.string.manual_pair))
                              }
                              if (showManual) {
                                  var ipF by remember { mutableStateOf("") }
                                  var portF by remember { mutableStateOf("") }
                                  var tokenF by remember { mutableStateOf("") }
                                  var fpF by remember { mutableStateOf("") }
                                  AlertDialog(
                                      onDismissRequest = { showManual = false },
                                      title = { Text(stringResource(R.string.manual_pair)) },
                                      text = {
                                          Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                              OutlinedTextField(value = ipF, onValueChange = { ipF = it }, label = { Text("IP") }, singleLine = true)
                                              OutlinedTextField(value = portF, onValueChange = { portF = it }, label = { Text(stringResource(R.string.manual_port)) }, singleLine = true)
                                              OutlinedTextField(value = tokenF, onValueChange = { tokenF = it }, label = { Text("Token") }, singleLine = true)
                                              OutlinedTextField(value = fpF, onValueChange = { fpF = it }, label = { Text(stringResource(R.string.manual_fp)) }, singleLine = true)
                                          }
                                      },
                                      confirmButton = {
                                          TextButton(onClick = {
                                              showManual = false
                                              vm.pairManual(ipF.trim(), portF.toIntOrNull() ?: 0, tokenF.trim(), fpF.trim())
                                          }) { Text(stringResource(R.string.connect)) }
                                      },
                                      dismissButton = {
                                          TextButton(onClick = { showManual = false }) { Text(stringResource(R.string.cancel)) }
                                      },
                                  )
                              }
                              var showP2p by remember { mutableStateOf(false) }
                              TextButton(onClick = { showP2p = !showP2p }, modifier = Modifier.padding(horizontal = Spacing.xl)) {
                                  Text(stringResource(R.string.p2p_connect))
                              }
                              if (showP2p) {
                                  val p2p = remember { com.linkdeck.app.pairing.P2pManager(context) }
                                  androidx.compose.runtime.LaunchedEffect(showP2p) { p2p.startDiscovery() }
                                  androidx.compose.runtime.DisposableEffect(showP2p) { onDispose { p2p.stopDiscovery() } }
                                  val peers by p2p.devices.collectAsState()
                                  val info by p2p.connectionInfo.collectAsState()
                                  var p2pCode by remember { mutableStateOf("") }
                                  Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                      Text(
                                          stringResource(R.string.p2p_hint),
                                          style = MaterialTheme.typography.bodySmall,
                                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      )
                                      peers.forEach { d ->
                                          TextButton(onClick = { p2p.connect(d) }) { Text(d.deviceName ?: d.deviceAddress) }
                                      }
                                      if (peers.isEmpty()) {
                                          Text(
                                              stringResource(R.string.p2p_empty),
                                              style = MaterialTheme.typography.bodySmall,
                                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                                          )
                                      }
                                      info?.let { i ->
                                          if (i.groupFormed) {
                                              OutlinedTextField(
                                                  value = p2pCode, onValueChange = { p2pCode = it },
                                                  label = { Text(stringResource(R.string.p2p_enter_code)) },
                                                  singleLine = true,
                                              )
                                              Button(onClick = {
                                                  val host = i.groupOwnerAddress?.hostAddress
                                                  if (host != null) vm.pairOverP2p(host, com.linkdeck.core.network.P2P_PORT, p2pCode.trim())
                                              }) { Text(stringResource(R.string.connect)) }
                                          }
                                      }
                                  }
                              }
                              CameraQrView(onCode = vm::onScanned, modifier = Modifier.weight(1f))
                          }
                          ScanUiState.Pairing -> {
                              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                      LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                                      Spacer(Modifier.height(Spacing.lg))
                                      Text(stringResource(R.string.pairing_in_progress), style = MaterialTheme.typography.titleMedium)
                                  }
                              }
                          }
                          is ScanUiState.Done -> {
                              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                      Text(
                                          stringResource(R.string.pairing_success, (state as ScanUiState.Done).deviceName),
                                          style = MaterialTheme.typography.titleMedium,
                                          color = MaterialTheme.colorScheme.primary,
                                      )
                                      Spacer(Modifier.height(Spacing.lg))
                                      Button(onClick = onBack) { Text(stringResource(R.string.ok)) }
                                  }
                              }
                          }
                          is ScanUiState.Error -> {
                              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                      Text(
                                          stringResource(R.string.pairing_failed),
                                          style = MaterialTheme.typography.titleMedium,
                                          color = MaterialTheme.colorScheme.error,
                                      )
                                      Spacer(Modifier.height(Spacing.sm))
                                      val errMsg = (state as ScanUiState.Error).message
                                      val clipboard = LocalClipboardManager.current
                                      Text(
                                          errMsg,
                                          style = MaterialTheme.typography.bodySmall,
                                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      )
                                      TextButton(onClick = { clipboard.setText(AnnotatedString(errMsg)) }) {
                                          Text(stringResource(R.string.copy_error))
                                      }
                                      var showLog by remember { mutableStateOf(false) }
                                      TextButton(onClick = { showLog = !showLog }) {
                                          Text(stringResource(R.string.view_log))
                                      }
                                      TextButton(onClick = { vm.fetchHostLog() }) {
                                          Text(stringResource(R.string.view_host_log))
                                      }
                                      val hostLog by vm.hostLog.collectAsState()
                                      hostLog?.let { hl ->
                                          androidx.compose.foundation.text.selection.SelectionContainer {
                                              Text(
                                                  hl,
                                                  style = MaterialTheme.typography.bodySmall,
                                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                  modifier = Modifier.padding(horizontal = Spacing.xl),
                                              )
                                          }
                                      }
                                      if (showLog) {
                                          androidx.compose.foundation.text.selection.SelectionContainer {
                                              Text(
                                                  com.linkdeck.core.network.DebugLog.dump(),
                                                  style = MaterialTheme.typography.bodySmall,
                                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                  modifier = Modifier.padding(horizontal = Spacing.xl),
                                              )
                                          }
                                      }
                                      Spacer(Modifier.height(Spacing.lg))
                                      Button(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
                                  }
                              }
                          }
                      }
                  }
              }
          }
          @Composable
          private fun CameraQrView(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
              val context = LocalContext.current
              val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
              val executor = remember { ContextCompat.getMainExecutor(context) }
              var bound by remember { mutableStateOf(false) }
              androidx.compose.ui.viewinterop.AndroidView(
                  factory = { ctx ->
                      PreviewView(ctx).apply {
                          scaleType = PreviewView.ScaleType.FILL_CENTER
                          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                      }
                  },
                  modifier = modifier,
              ) { previewView ->
                  if (bound) return@AndroidView
                  bound = true
                  val providerFuture = ProcessCameraProvider.getInstance(context)
                  providerFuture.addListener({
                      val provider = providerFuture.get()
                      val preview = Preview.Builder().build().also {
                          it.setSurfaceProvider(previewView.surfaceProvider)
                      }
                      val analysis = ImageAnalysis.Builder()
                          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                          .build()
                          .also { it.setAnalyzer(executor, QrAnalyzer(onCode)) }
                      runCatching {
                          provider.unbindAll()
                          provider.bindToLifecycle(
                              lifecycleOwner,
                              CameraSelector.DEFAULT_BACK_CAMERA,
                              preview,
                              analysis,
                          )
                      }
                  }, executor)
              }
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/home
          cat > app/src/main/java/com/linkdeck/app/ui/home/HostHomeScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.home
          import androidx.compose.foundation.background
          import androidx.activity.compose.rememberLauncherForActivityResult
          import androidx.activity.result.contract.ActivityResultContracts
          import androidx.compose.foundation.layout.Arrangement
          import androidx.compose.foundation.layout.Box
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Row
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.height
          import androidx.compose.foundation.layout.padding
          import androidx.compose.foundation.layout.size
          import androidx.compose.foundation.layout.width
          import androidx.compose.foundation.lazy.LazyColumn
          import androidx.compose.foundation.shape.CircleShape
          import androidx.compose.foundation.shape.RoundedCornerShape
          import androidx.compose.material3.Button
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.OutlinedButton
          import androidx.compose.material3.Surface
          import androidx.compose.material3.Text
          import androidx.compose.material3.TextButton
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.collectAsState
          import androidx.compose.ui.text.AnnotatedString
          import androidx.compose.ui.platform.LocalClipboardManager
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.remember
          import androidx.compose.runtime.setValue
          import androidx.compose.runtime.getValue
          import androidx.compose.ui.Alignment
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.platform.LocalContext
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.unit.dp
          import androidx.hilt.navigation.compose.hiltViewModel
          import androidx.lifecycle.ViewModel
          import com.linkdeck.app.R
          import com.linkdeck.app.pairing.PairingManager
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.LinkDeckTheme
          import com.linkdeck.core.design.Spacing
          import com.linkdeck.core.network.ConnectionManager
          import com.linkdeck.core.network.HostServer
          import dagger.hilt.android.lifecycle.HiltViewModel
          import javax.inject.Inject
          @HiltViewModel
          class HostHomeViewModel @Inject constructor(
              val pairingManager: PairingManager,
              val hostServer: HostServer,
              val connectionManager: ConnectionManager,
          ) : ViewModel()
          @Composable
          fun HostHomeScreen(
              onShowQr: () -> Unit,
              onOpenProWizard: () -> Unit,
              onOpenPermissions: () -> Unit,
              onOpenSettings: () -> Unit,
          ) {
              val vm: HostHomeViewModel = hiltViewModel()
              val hostEvent by vm.hostServer.events.collectAsState()
              val beingControlled = vm.hostServer.connectedCount > 0
              Box(Modifier.fillMaxSize()) {
                  Column(
                      Modifier.fillMaxSize().padding(Spacing.xxl),
                      horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                      Spacer(Modifier.height(Spacing.xxl))
                      Box(contentAlignment = Alignment.Center) {
                          Surface(
                              shape = CircleShape,
                              color = if (beingControlled) LinkDeckTheme.tints.dangerBg else LinkDeckTheme.tints.accentBg,
                              modifier = Modifier.size(160.dp),
                          ) {}
                          LineIconView(
                              icon = LineIcon.POWER,
                              tint = if (beingControlled) LinkDeckTheme.tints.dangerFg else LinkDeckTheme.tints.accentFg,
                              size = 64.dp,
                          )
                      }
                      Spacer(Modifier.height(Spacing.md))
                      Text(
                          text = if (beingControlled) stringResource(R.string.host_sharing_active)
                          else stringResource(R.string.host_waiting),
                          style = MaterialTheme.typography.titleMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Spacer(Modifier.height(Spacing.xxl))
                      Button(onClick = {
                          vm.pairingManager.hostStart()
                          onShowQr()
                      }) { Text(stringResource(R.string.show_qr)) }
                      Spacer(Modifier.height(Spacing.sm))
                      OutlinedButton(onClick = onOpenProWizard) { Text(stringResource(R.string.pro_title)) }
                      val context = LocalContext.current
                      var hotMsg by remember { mutableStateOf<String?>(null) }
                      fun startHotspot() {
                          com.linkdeck.app.host.HostHotspotManager(context).start(
                              object : com.linkdeck.app.host.HostHotspotManager.Callback {
                                  override fun onStarted(info: String) {
                                      hotMsg = info
                                      vm.pairingManager.rotateQrToken()
                                      onShowQr()
                                  }
                                  override fun onError(reason: String) { hotMsg = reason }
                              },
                          )
                      }
                      val permLauncher = rememberLauncherForActivityResult(
                          ActivityResultContracts.RequestPermission(),
                      ) { granted ->
                          if (granted) startHotspot() else hotMsg = context.getString(R.string.hotspot_need_location)
                      }
                      Spacer(Modifier.height(Spacing.sm))
                      OutlinedButton(onClick = {
                          val needLoc = android.os.Build.VERSION.SDK_INT < 29 &&
                              androidx.core.content.ContextCompat.checkSelfPermission(
                                  context, android.Manifest.permission.ACCESS_FINE_LOCATION,
                              ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                          if (needLoc) permLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                          else startHotspot()
                      }) { Text(stringResource(R.string.hotspot_start)) }
                      hotMsg?.let {
                          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      val p2p = remember { com.linkdeck.app.pairing.P2pManager(context) }
                      var p2pCode by remember { mutableStateOf<String?>(null) }
                      val p2pInfo by p2p.connectionInfo.collectAsState()
                      androidx.compose.runtime.LaunchedEffect(p2pInfo) {
                          if (p2pInfo?.isGroupOwner == true && p2pCode == null) {
                              p2pCode = vm.pairingManager.hostStartP2p()
                          }
                      }
                      Spacer(Modifier.height(Spacing.sm))
                      OutlinedButton(onClick = {
                          p2pCode = null
                          p2p.startDiscovery()
                          p2p.createGroup()
                      }) { Text(stringResource(R.string.p2p_host_start)) }
                      p2pCode?.let {
                          Text(
                              stringResource(R.string.p2p_code_is, it),
                              style = MaterialTheme.typography.titleMedium,
                              color = MaterialTheme.colorScheme.primary,
                          )
                      }
                      Spacer(Modifier.height(Spacing.lg))
                      Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                          OutlinedButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }
                          OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
                      }
                      Spacer(Modifier.height(Spacing.sm))
                      var showLog by remember { mutableStateOf(false) }
                      TextButton(onClick = { showLog = !showLog }) { Text(stringResource(R.string.view_log)) }
                      if (showLog) {
                          val clip = LocalClipboardManager.current
                          androidx.compose.material3.AlertDialog(
                              onDismissRequest = { showLog = false },
                              title = { Text(stringResource(R.string.view_log)) },
                              text = {
                                  Column {
                                      androidx.compose.foundation.text.selection.SelectionContainer {
                                          Text(
                                              com.linkdeck.core.network.DebugLog.dump().ifEmpty { "log is empty" },
                                              style = MaterialTheme.typography.bodySmall,
                                          )
                                      }
                                      TextButton(onClick = {
                                          clip.setText(AnnotatedString(com.linkdeck.core.network.DebugLog.dump()))
                                      }) { Text(stringResource(R.string.copy_error)) }
                                  }
                              },
                              confirmButton = {
                                  TextButton(onClick = { showLog = false }) { Text(stringResource(R.string.ok)) }
                              },
                          )
                      }
                      Spacer(Modifier.height(Spacing.xxl))
                      Text(
                          stringResource(R.string.trusted_devices),
                          style = MaterialTheme.typography.titleMedium,
                          modifier = Modifier.fillMaxWidth(),
                      )
                      Spacer(Modifier.height(Spacing.sm))
                      LazyColumn(Modifier.fillMaxWidth()) {
                          item {
                              Text(
                                  text = stringResource(R.string.host_controllers_hint, vm.hostServer.pairedDeviceIds().size),
                                  style = MaterialTheme.typography.bodyMedium,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                              )
                          }
                      }
                  }
                  if (beingControlled) {
                      Surface(
                          color = MaterialTheme.colorScheme.surface,
                          shape = RoundedCornerShape(50),
                          tonalElevation = 6.dp,
                          modifier = Modifier
                              .align(Alignment.BottomCenter)
                              .padding(Spacing.xl)
                              .fillMaxWidth(),
                      ) {
                          Row(
                              Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                              verticalAlignment = Alignment.CenterVertically,
                          ) {
                              Box(
                                  Modifier
                                      .size(10.dp)
                                      .background(LinkDeckTheme.tints.dangerFg, CircleShape)
                              )
                              Spacer(Modifier.width(Spacing.sm))
                              Text(
                                  stringResource(R.string.being_controlled),
                                  style = MaterialTheme.typography.labelLarge,
                                  modifier = Modifier.weight(1f),
                              )
                              TextButton(onClick = {
                                  vm.hostServer.pairedDeviceIds().forEach { vm.hostServer.revokeDevice(it) }
                              }) { Text(stringResource(R.string.emergency_disconnect), color = LinkDeckTheme.tints.dangerFg) }
                          }
                      }
                  }
              }
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/home
          cat > app/src/main/java/com/linkdeck/app/ui/home/ControllerHomeScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.home
          import androidx.compose.foundation.layout.Box
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Row
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.height
          import androidx.compose.foundation.layout.padding
          import androidx.compose.foundation.layout.width
          import androidx.compose.foundation.lazy.LazyColumn
          import androidx.compose.foundation.lazy.items
          import androidx.compose.foundation.shape.RoundedCornerShape
          import androidx.compose.material3.Button
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.NavigationRail
          import androidx.compose.material3.NavigationRailItem
          import androidx.compose.material3.Surface
          import androidx.compose.material3.Text
          import androidx.compose.material3.TextButton
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.collectAsState
          import androidx.compose.runtime.getValue
          import androidx.compose.ui.Alignment
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.platform.LocalContext
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.unit.dp
          import androidx.hilt.navigation.compose.hiltViewModel
          import androidx.lifecycle.ViewModel
          import com.linkdeck.app.R
          import com.linkdeck.app.devices.TrustedDeviceStore
          import com.linkdeck.app.pairing.PairingManager
          import com.linkdeck.app.ui.settings.FeatureCatalog
          import com.linkdeck.app.voice.VoiceCommandManager
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.LinkDeckTheme
          import com.linkdeck.core.design.Spacing
          import com.linkdeck.core.design.components.StatusChip
          import com.linkdeck.core.design.components.StatusChips
          import com.linkdeck.core.network.ConnectionManager
          import com.linkdeck.core.network.ConnectionState
          import dagger.hilt.android.lifecycle.HiltViewModel
          import javax.inject.Inject
          private data class RailEntry(val route: String, val icon: LineIcon, val labelRes: Int)
          @Composable
          fun ControllerHomeScreen(
              onScanQr: () -> Unit,
              onNavigate: (String) -> Unit,
              onOpenPermissions: () -> Unit,
              onOpenSettings: () -> Unit,
          ) {
              Row(Modifier.fillMaxSize()) {
                  val context = LocalContext.current
                  NavigationRail {
                      FeatureCatalog.visible(context)
                          .map { RailEntry(it.id, it.icon, it.labelRes) }
                          .forEach { entry ->
                              NavigationRailItem(
                                  selected = false,
                                  onClick = { onNavigate(entry.route) },
                                  icon = {
                                      LineIconView(
                                          icon = entry.icon,
                                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                          size = 24.dp,
                                      )
                                  },
                                  label = { Text(stringResource(entry.labelRes), style = MaterialTheme.typography.labelSmall) },
                              )
                          }
                  }
                  Column(Modifier.weight(1f)) {
                      ControllerHomeContent(
                          onScanQr = onScanQr,
                          onOpenPermissions = onOpenPermissions,
                      )
                      VoiceFab(onRoute = onNavigate)
                  }
              }
          }
          @Composable
          private fun ControllerHomeContent(
              onScanQr: () -> Unit,
              onOpenPermissions: () -> Unit,
              modifier: Modifier = Modifier,
          ) {
              val vm: ControllerHomeViewModel = hiltViewModel()
              val devices by vm.trustedStore.devices.collectAsState()
              val connectionState by vm.connectionManager.state.collectAsState()
              Column(modifier.fillMaxSize().padding(Spacing.xxl)) {
                  Text(stringResource(R.string.controller_title), style = MaterialTheme.typography.headlineSmall)
                  Spacer(Modifier.height(Spacing.sm))
                  ConnectionStatusLine(state = connectionState)
                  Spacer(Modifier.height(Spacing.xl))
                  Button(onClick = onScanQr) { Text(stringResource(R.string.scan_qr)) }
                  Spacer(Modifier.height(Spacing.sm))
                  TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }
                  Spacer(Modifier.height(Spacing.xxl))
                  Text(stringResource(R.string.trusted_devices), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(Spacing.sm))
                  if (devices.isEmpty()) {
                      Text(
                          stringResource(R.string.empty_devices),
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                  } else {
                      LazyColumn {
                          items(devices, key = { it.id }) { device ->
                              Surface(
                                  color = MaterialTheme.colorScheme.surfaceVariant,
                                  shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
                                  modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                              ) {
                                  Row(
                                      Modifier.padding(Spacing.xl),
                                      verticalAlignment = Alignment.CenterVertically,
                                  ) {
                                      LineIconView(
                                          icon = LineIcon.DEVICES,
                                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                          size = 28.dp,
                                      )
                                      Spacer(Modifier.width(Spacing.md))
                                      Column(Modifier.weight(1f)) {
                                          Text(device.name, style = MaterialTheme.typography.titleMedium)
                                          Text(
                                              device.model,
                                              style = MaterialTheme.typography.bodySmall,
                                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                                          )
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
          }
          @Composable
          private fun ConnectionStatusLine(state: ConnectionState) {
              val (text, fg) = when (state) {
                  ConnectionState.Disconnected ->
                      stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
                  ConnectionState.Connecting ->
                      stringResource(R.string.state_connecting) to LinkDeckTheme.tints.warningFg
                  is ConnectionState.Connected ->
                      stringResource(R.string.state_connected) to LinkDeckTheme.tints.successFg
                  is ConnectionState.Error ->
                      stringResource(R.string.state_error) to LinkDeckTheme.tints.dangerFg
              }
              when (state) {
                  is ConnectionState.Connected -> StatusChips.Success(text)
                  ConnectionState.Connecting -> StatusChips.Warning(text)
                  is ConnectionState.Error -> StatusChips.Danger(text)
                  ConnectionState.Disconnected -> StatusChip(
                      text,
                      MaterialTheme.colorScheme.surfaceVariant,
                      fg,
                  )
              }
          }
          @HiltViewModel
          class ControllerHomeViewModel @Inject constructor(
              val trustedStore: TrustedDeviceStore,
              val connectionManager: ConnectionManager,
              val pairingManager: PairingManager,
              val voice: VoiceCommandManager,
          ) : ViewModel()
          @Composable
          private fun VoiceFab(onRoute: (String) -> Unit) {
              val vm: ControllerHomeViewModel = hiltViewModel()
              val state by vm.voice.state.collectAsState()
              Box(Modifier.padding(Spacing.xl)) {
                  Button(onClick = {
                      val lang = java.util.Locale.getDefault().language.take(2).ifBlank { "en" }
                      vm.voice.ensureModelAndStart(lang, onRoute)
                  }) {
                      Text(
                          when (state) {
                              is VoiceCommandManager.VoiceState.Downloading ->
                                  stringResource(R.string.voice_download)
                              VoiceCommandManager.VoiceState.Listening ->
                                  stringResource(R.string.voice_listening)
                              else -> stringResource(R.string.voice_start)
                          }
                      )
                  }
              }
          }
          KOTLIN_EOF
          mkdir -p app/src/main/java/com/linkdeck/app/ui/devices
          cat > app/src/main/java/com/linkdeck/app/ui/devices/DevicesScreen.kt <<'KOTLIN_EOF'
          package com.linkdeck.app.ui.devices
          import androidx.compose.foundation.layout.Column
          import androidx.compose.foundation.layout.Row
          import androidx.compose.foundation.layout.Spacer
          import androidx.compose.foundation.layout.fillMaxSize
          import androidx.compose.foundation.layout.fillMaxWidth
          import androidx.compose.foundation.layout.padding
          import androidx.compose.foundation.layout.width
          import androidx.compose.foundation.lazy.LazyColumn
          import androidx.compose.foundation.lazy.items
          import androidx.compose.foundation.shape.RoundedCornerShape
          import androidx.compose.material3.Button
          import androidx.compose.material3.ExperimentalMaterial3Api
          import androidx.compose.material3.IconButton
          import androidx.compose.material3.MaterialTheme
          import androidx.compose.material3.Scaffold
          import androidx.compose.material3.Surface
          import androidx.compose.material3.Text
          import androidx.compose.material3.TextButton
          import androidx.compose.material3.TopAppBar
          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.collectAsState
          import androidx.compose.runtime.getValue
          import androidx.compose.ui.Alignment
          import androidx.compose.ui.Modifier
          import androidx.compose.ui.res.stringResource
          import androidx.compose.ui.unit.dp
          import androidx.hilt.navigation.compose.hiltViewModel
          import androidx.lifecycle.ViewModel
          import com.linkdeck.app.R
          import com.linkdeck.app.devices.TrustedDeviceStore
          import com.linkdeck.core.design.LineIcon
          import com.linkdeck.core.design.LineIconView
          import com.linkdeck.core.design.LinkDeckTheme
          import com.linkdeck.core.design.Spacing
          import com.linkdeck.core.network.ConnectionManager
          import com.linkdeck.core.network.TrustedDevice
          import dagger.hilt.android.lifecycle.HiltViewModel
          import javax.inject.Inject
          @HiltViewModel
          class DevicesViewModel @Inject constructor(
              val trustedStore: TrustedDeviceStore,
              val connectionManager: ConnectionManager,
          ) : ViewModel() {
              fun connect(device: TrustedDevice) = connectionManager.connect(device)
              fun disconnect() = connectionManager.disconnect()
              fun revoke(device: TrustedDevice) {
                  connectionManager.disconnect()
                  trustedStore.remove(device.id)
              }
          }
          @OptIn(ExperimentalMaterial3Api::class)
          @Composable
          fun DevicesScreen(onBack: () -> Unit, onOpenSuper: () -> Unit = {}) {
              val vm: DevicesViewModel = hiltViewModel()
              val devices by vm.trustedStore.devices.collectAsState()
              val connectionState by vm.connectionManager.state.collectAsState()
              Scaffold(
                  topBar = {
                      TopAppBar(
                          title = { Text(stringResource(R.string.nav_devices)) },
                          actions = {
                              TextButton(onClick = onOpenSuper) {
                                  LineIconView(icon = LineIcon.DEVICES, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                                  Spacer(Modifier.width(Spacing.xs))
                                  Text(stringResource(R.string.superdevice_title), color = MaterialTheme.colorScheme.primary)
                              }
                          },
                          navigationIcon = {
                              IconButton(onClick = onBack) {
                                  LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                              }
                          },
                      )
                  },
              ) { padding ->
                  Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl)) {
                      if (devices.isEmpty()) {
                          Text(
                              stringResource(R.string.empty_devices),
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                      }
                      LazyColumn {
                          items(devices, key = { it.id }) { device ->
                              val connected = (connectionState as? com.linkdeck.core.network.ConnectionState.Connected)?.device?.id == device.id
                              Surface(
                                  color = MaterialTheme.colorScheme.surfaceVariant,
                                  shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
                                  modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                              ) {
                                  Row(
                                      Modifier.padding(Spacing.xl),
                                      verticalAlignment = Alignment.CenterVertically,
                                  ) {
                                      LineIconView(icon = LineIcon.DEVICES, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 32.dp)
                                      Spacer(Modifier.width(Spacing.md))
                                      Column(Modifier.weight(1f)) {
                                          Text(device.name, style = MaterialTheme.typography.titleMedium)
                                          Text(
                                              device.model,
                                              style = MaterialTheme.typography.bodySmall,
                                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                                          )
                                      }
                                      if (connected) {
                                          TextButton(onClick = { vm.disconnect() }) { Text(stringResource(R.string.disconnect)) }
                                      } else {
                                          Button(onClick = { vm.connect(device) }) { Text(stringResource(R.string.connect)) }
                                      }
                                      TextButton(onClick = { vm.revoke(device) }) {
                                          Text(stringResource(R.string.revoke), color = LinkDeckTheme.tints.dangerFg)
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
          }
          KOTLIN_EOF
          mkdir -p .
          cat > DESIGN.md <<'KOTLIN_EOF'
          # LinkDeck — DESIGN.md
          ## وضع خدمات جوجل (إلزامي)
          ## المزاج البصري (Moodboard)
          "هدوء فائق الجودة": Super-Device بدون ضجيج — مساحات بيضاء دافئة، ألوان معتمدة من نظام التصميم، أيقونات خطية مرسومة يدويًا، مفاهيم: الاتصال المحلي، الثقة، البساطة.
          ## Design Tokens (core-design)
          - السطوح: فاتح #F8F7F3/#FDFDFB — داكن #0F100E/#131412
          - التمائج الستة: Default/Teal/Indigo/Amber/Rose/Dynamic (Material You) — تباين كل زوج مضمون ≥ 4.5:1
          - الخطوط المضمنة: Cairo، IBM Plex Arabic (بديل Tajawal)، Madika، Basseet، The Year of the Camel + خط النظام
          - الأيقونات: LineIcon grid 64، stroke قابل للتخصيص (Thin/Regular/Bold)
          ## المنافسون وتحليلهم
          Super Device (Huawei)، Phone Link (Microsoft)، AirDrop (Apple)، LocalSend، KDE Connect
          الميزة التوقيعية: وضع Host/Controller في APK واحد + Super-Device-style drag-to-connect + ميرايا نافذة عائمة.
          ## Mockups
          المعتمدة تفاعليًا داخل المحادثة (معاينات الشاشات الثماني + شاشة السحب والإفلات).
          ## بنية الاتصال
          QR/TLS-pinning (LAN) · Hotspot خاص بالتطبيق · Wi-Fi Direct (P2P) · إدخال يدوي · mDNS اكتشاف تلقائي.
          KOTLIN_EOF
          mkdir -p .
          cat > BFB.txt <<'KOTLIN_EOF'
          Built For Benefits, by AI — BFB.AI
          ==================================
          LinkDeck is Built For Benefits, by AI (BFB.AI).
          Build chain: AI-assisted engineering with human approval gates.
          Brand assets: assets/brand/ (SVG ×3: light/dark/mono).
          This file ships inside every release archive.
          KOTLIN_EOF
          mkdir -p app/src/main/assets/brand
          cat > app/src/main/assets/brand/bfb-logo-light.svg <<'KOTLIN_EOF'
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64"><path d="M24 8 h16 a8 8 0 0 1 0 16 h-16 z M24 24 h20 a8 8 0 0 1 0 16 h-20 z M24 40 h24 a8 8 0 0 1 0 16 h-24 z" fill="none" stroke="#1B1C1A" stroke-width="4" stroke-linejoin="round"/><circle cx="20" cy="16" r="4" fill="#1B1C1A"/><circle cx="20" cy="32" r="4" fill="#1B1C1A"/><circle cx="20" cy="48" r="4" fill="#1B1C1A"/></svg>
          KOTLIN_EOF
          mkdir -p app/src/main/assets/brand
          cat > app/src/main/assets/brand/bfb-logo-dark.svg <<'KOTLIN_EOF'
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64"><path d="M24 8 h16 a8 8 0 0 1 0 16 h-16 z M24 24 h20 a8 8 0 0 1 0 16 h-20 z M24 40 h24 a8 8 0 0 1 0 16 h-24 z" fill="none" stroke="#E4E3DD" stroke-width="4" stroke-linejoin="round"/><circle cx="20" cy="16" r="4" fill="#E4E3DD"/><circle cx="20" cy="32" r="4" fill="#E4E3DD"/><circle cx="20" cy="48" r="4" fill="#E4E3DD"/></svg>
          KOTLIN_EOF
          mkdir -p app/src/main/assets/brand
          cat > app/src/main/assets/brand/bfb-logo-mono.svg <<'KOTLIN_EOF'
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64"><path d="M24 8 h16 a8 8 0 0 1 0 16 h-16 z M24 24 h20 a8 8 0 0 1 0 16 h-20 z M24 40 h24 a8 8 0 0 1 0 16 h-24 z" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-linejoin="round"/><circle cx="20" cy="16" r="4" fill="#FFFFFF"/><circle cx="20" cy="32" r="4" fill="#FFFFFF"/><circle cx="20" cy="48" r="4" fill="#FFFFFF"/></svg>
          KOTLIN_EOF
      - name: README brand
        run: |
          python3 - <<'PYEOF'
          import pathlib, sys
          p = pathlib.Path("README.md")
          t = p.read_text(encoding="utf-8")
          if "BFB.AI" not in t and "# LinkDeck" in t:
              p.write_text(t.replace("# LinkDeck", "# LinkDeck\n\n> Built For Benefits, by AI — **BFB.AI**", 1), encoding="utf-8")
              print("patched: README")
          else:
              print("skip: README")
          PYEOF
      - name: BFB compliance verify
        run: |
          python3 - <<'PYEOF'
          import pathlib, sys
          checks = [
              ("receive() client", "app/src/main/java/com/linkdeck/app/pairing/PairingManager.kt", "incoming.receive()"),
              ("P2P pairing", "app/src/main/java/com/linkdeck/app/pairing/PairingManager.kt", "pairOverP2p"),
              ("P2P port", "core-network/src/main/java/com/linkdeck/core/network/P2pMessages.kt", "P2P_PORT"),
              ("P2P manager", "app/src/main/java/com/linkdeck/app/pairing/P2pManager.kt", "WifiP2pManager"),
              ("DebugLog", "core-network/src/main/java/com/linkdeck/core/network/DebugLog.kt", "object DebugLog"),
              ("Server always replies", "core-network/src/main/java/com/linkdeck/core/network/HostServer.kt", "p2p_auth_ok"),
              ("Root check", "app/src/main/java/com/linkdeck/app/security/SecurityCheck.kt", "isDeviceRooted"),
              ("Crash share", "app/src/main/java/com/linkdeck/app/MainActivity.kt", "crash_share"),
              ("TLS key protected", "core-network/src/main/java/com/linkdeck/core/network/Tls.kt", "linkdeck_tls"),
              ("DESIGN.md", "DESIGN.md", "No-GMS"),
              ("BFB.txt", "BFB.txt", "BFB.AI"),
              ("Fonts", "core-design/src/main/java/com/linkdeck/core/design/Type.kt", "IBM_PLEX"),
              ("Manual pair", "app/src/main/java/com/linkdeck/app/ui/pairing/ScanQrScreen.kt", "pairManual"),
              ("Hotspot", "app/src/main/java/com/linkdeck/app/host/HostHotspotManager.kt", "startLocalOnlyHotspot"),
              ("Diag log button", "app/src/main/java/com/linkdeck/app/ui/home/HostHomeScreen.kt", "view_log"),
          ]
          fails = []
          for label, path, needle in checks:
              ok = pathlib.Path(path).exists() and needle in pathlib.Path(path).read_text(encoding="utf-8")
              print(("PASS " if ok else "FAIL ") + label)
              if not ok: fails.append(label)
          print("BFB compliance:", "ALL PASS ✓" if not fails else "FAILURES: " + ", ".join(fails))
          sys.exit(1 if fails else 0)
          PYEOF
      - name: Commit and push
        run: |
          git config user.name "Mohammedazizhedia"
          git config user.email "mohammedazizhedia@gmail.com"
          git add -A
          git commit -m "Rebuild UI + BFB docs + compliance verification"
          git push
