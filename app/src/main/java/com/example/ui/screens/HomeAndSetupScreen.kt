package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ime.KhattInputMethodService
import com.example.model.KeyboardSettings
import com.example.ui.components.LiveKeyboardPreview
import com.example.ui.viewmodel.SettingsViewModel

data class DashboardSectionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: Any,
)

@Composable
fun HomeAndSetupScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
    onNavigate: (Any) -> Unit,
    sections: List<DashboardSectionItem>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isImeEnabled by remember { mutableStateOf(checkImeEnabled(context)) }
    var isImeSelected by remember { mutableStateOf(checkImeSelected(context)) }
    var testText by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isImeEnabled = checkImeEnabled(context)
                isImeSelected = checkImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Banner Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0F1322))
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Column {
                            Text(
                                text = "كيبورد خط المتكامل",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "كتابة سريعة • ٩ لغات • حافظة ذكية • ترجمة وزخرفة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD6E4FF),
                            )
                        }
                    }
                }
            }
        }

        // First-launch / Status Setup Wizard Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (isImeEnabled && isImeSelected) "الكيبورد جاهز للكتابة ✓" else "معالج التفعيل السريع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("enable_ime_button"),
                        ) {
                            if (isImeEnabled) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isImeEnabled) "١. مفعّل" else "١. تفعيل الكيبورد")
                        }

                        OutlinedButton(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showInputMethodPicker()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_ime_button"),
                        ) {
                            if (isImeSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isImeSelected) "٢. افتراضي" else "٢. اختيار كافتراضي")
                        }
                    }

                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("جرّب الكتابة هنا مباشرة...") },
                        placeholder = { Text("اضغط لفتح لوحة المفاتيح وتجربة السحب والاختصارات") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_typing_input"),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
        }

        // Live Keyboard Preview
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "معاينة حية للإعدادات الحالية",
                    style = MaterialTheme.typography.titleMedium,
                )
                LiveKeyboardPreview(
                    settings = settings,
                    layoutRepo = viewModel.layoutRepo,
                )
            }
        }

        // Settings Categories
        item {
            Text(
                text = "أقسام التخصيص والإعدادات",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(sections.size) { index ->
            val section = sections[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigate(section.route) }
                    .testTag("section_card_${section.id}"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            section.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun checkImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    val pkg = context.packageName
    return imm.enabledInputMethodList.any { it.packageName == pkg }
}

private fun checkImeSelected(context: Context): Boolean {
    val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
    return current.contains(KhattInputMethodService::class.java.simpleName) ||
        current.startsWith(context.packageName)
}
