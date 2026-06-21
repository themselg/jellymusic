// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.profile

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.data.prefs.ColorMode
import dev.themselg.jellymusic.data.prefs.DarkMode
import dev.themselg.jellymusic.data.prefs.ThemeSettings
import dev.themselg.jellymusic.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val serverName by viewModel.serverName.collectAsStateWithLifecycle()
    val userImageUrl by viewModel.userImageUrl.collectAsStateWithLifecycle()
    val playlistCount by viewModel.playlistCount.collectAsStateWithLifecycle()
    val likedCount by viewModel.likedCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileHeader(
                userName = userName,
                serverName = serverName,
                userImageUrl = userImageUrl,
            )

            StatsRow(playlistCount = playlistCount, likedCount = likedCount)

            // Signature feature: highlighted color-mode card.
            ColorModeSection(selected = settings.colorMode, onSelect = viewModel::setColorMode)

            SectionHeader(stringResource(R.string.settings_appearance))
            DarkModeRow(DarkMode.SYSTEM, R.string.dark_mode_system, settings.darkMode, viewModel::setDarkMode)
            DarkModeRow(DarkMode.LIGHT, R.string.dark_mode_light, settings.darkMode, viewModel::setDarkMode)
            DarkModeRow(DarkMode.DARK, R.string.dark_mode_dark, settings.darkMode, viewModel::setDarkMode)

            AmoledRow(checked = settings.amoledBlack, onCheckedChange = viewModel::setAmoledBlack)

            SectionHeader(stringResource(R.string.settings_language))
            LanguageSection()

            SectionHeader(stringResource(R.string.about))
            AboutRow()
            AcknowledgementsRow()

            SectionHeader(stringResource(R.string.settings_account))
            OutlinedButton(
                onClick = viewModel::signOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Text(
                    text = stringResource(R.string.sign_out),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    serverName: String,
    userImageUrl: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (!userImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = userName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.8f),
                )
            }
        }
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (serverName.isNotBlank()) {
            Text(
                text = serverName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatsRow(
    playlistCount: Int,
    likedCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            value = playlistCount,
            labelRes = R.string.stat_playlists,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = likedCount,
            labelRes = R.string.stat_liked,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    value: Int,
    labelRes: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutRow() {
    val context = LocalContext.current
    // Read the version from the installed package so this module needs no BuildConfig from :app.
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The third-party projects JellyMusic is built with. Each opens its homepage when tapped. */
private val acknowledgements = listOf(
    "Jellyfin" to "https://jellyfin.org",
    "Jellyfin Kotlin SDK" to "https://github.com/jellyfin/jellyfin-sdk-kotlin",
    "Coil" to "https://github.com/coil-kt/coil",
    "OkHttp" to "https://github.com/square/okhttp",
    "MaterialKolor" to "https://github.com/jordond/MaterialKolor",
    "WavySlider" to "https://github.com/mahozad/wavy-slider",
)

/** A discreet About entry that opens the acknowledgements dialog. */
@Composable
private fun AcknowledgementsRow() {
    var show by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { show = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.acknowledgements), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.acknowledgements_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(stringResource(R.string.acknowledgements)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    acknowledgements.forEach { (name, url) ->
                        AcknowledgementItem(name = name, url = url)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun AcknowledgementItem(name: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorModeSection(
    selected: ColorMode,
    onSelect: (ColorMode) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_color_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.settings_color_mode_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
            ColorModeRow(
                mode = ColorMode.ALBUM_ART,
                titleRes = R.string.color_mode_album_art,
                captionRes = R.string.color_mode_album_art_caption,
                selected = selected,
                onSelect = onSelect,
            )
            ColorModeRow(
                mode = ColorMode.SYSTEM,
                titleRes = R.string.color_mode_system,
                captionRes = R.string.color_mode_system_caption,
                selected = selected,
                onSelect = onSelect,
            )
            ColorModeRow(
                mode = ColorMode.STATIC,
                titleRes = R.string.color_mode_static,
                captionRes = R.string.color_mode_static_caption,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun ColorModeRow(
    mode: ColorMode,
    titleRes: Int,
    captionRes: Int,
    selected: ColorMode,
    onSelect: (ColorMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected == mode,
                role = Role.RadioButton,
                onClick = { onSelect(mode) },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == mode, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(captionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DarkModeRow(
    mode: DarkMode,
    titleRes: Int,
    selected: DarkMode,
    onSelect: (DarkMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected == mode,
                role = Role.RadioButton,
                onClick = { onSelect(mode) },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == mode, onClick = null)
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AmoledRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_amoled), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.settings_amoled_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * In-app language picker. Uses [AppCompatDelegate] per-app locales, which apply immediately
 * (the activity is recreated) and are persisted (autoStoreLocales in the manifest). An empty
 * locale list means "follow the system language". Works from minSdk 31 upward.
 */
@Composable
private fun LanguageSection() {
    val options = listOf(
        "" to R.string.language_system,
        "en" to R.string.language_english,
        "es" to R.string.language_spanish,
        "de" to R.string.language_german,
        "fr" to R.string.language_french,
        "it" to R.string.language_italian,
        "nl" to R.string.language_dutch,
        "pl" to R.string.language_polish,
        "pt-BR" to R.string.language_portuguese_br,
        "pt-PT" to R.string.language_portuguese_pt,
        "ru" to R.string.language_russian,
        "uk" to R.string.language_ukrainian,
        "tr" to R.string.language_turkish,
        "zh-CN" to R.string.language_chinese_simplified,
        "ja" to R.string.language_japanese,
        "ko" to R.string.language_korean,
    )
    var currentTag by remember {
        // Match the active locale against the option tags, preferring the longest match so
        // region variants (pt-BR / pt-PT, zh-CN) win over a bare language prefix.
        val active = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val match = options.map { it.first }
            .filter { it.isNotEmpty() }
            .sortedByDescending { it.length }
            .firstOrNull { active.startsWith(it, ignoreCase = true) }
            .orEmpty()
        mutableStateOf(match)
    }
    var showDialog by remember { mutableStateOf(false) }
    val currentLabelRes = options.firstOrNull { it.first == currentTag }?.second
        ?: R.string.language_system

    // Discreet entry: a single row showing the current language; tap to open the picker dialog.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(currentLabelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { (tag, labelRes) ->
                        LanguageRow(
                            labelRes = labelRes,
                            selected = currentTag == tag,
                            onClick = {
                                currentTag = tag
                                val locales = if (tag.isEmpty()) {
                                    LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    LocaleListCompat.forLanguageTags(tag)
                                }
                                showDialog = false
                                AppCompatDelegate.setApplicationLocales(locales)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LanguageRow(
    labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
