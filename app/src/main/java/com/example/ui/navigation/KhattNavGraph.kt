package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.model.LanguageId
import com.example.ui.screens.BarsAndGesturesScreen
import com.example.ui.screens.ClipboardSettingsScreen
import com.example.ui.screens.CustomLayoutEditorScreen
import com.example.ui.screens.DashboardSectionItem
import com.example.ui.screens.DecorateAndFeedbackScreen
import com.example.ui.screens.DimensionsScreen
import com.example.ui.screens.HomeAndSetupScreen
import com.example.ui.screens.LanguagesAndLayoutScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.TranslationSettingsScreen
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteHome
@Serializable object RouteLanguagesAndLayout
@Serializable data class RouteCustomLayoutEditor(val languageName: String)
@Serializable object RouteDimensions
@Serializable object RouteThemes
@Serializable object RouteBarsAndGestures
@Serializable object RouteClipboard
@Serializable object RouteTranslation
@Serializable object RouteDecorate
@Serializable object RouteFeedback
@Serializable object RouteBackup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhattSettingsRoot(viewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        if (!statusMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMsg!!)
            viewModel.clearStatusMessage()
        }
    }

    val sections = remember {
        listOf(
            DashboardSectionItem(
                id = "languages",
                title = "اللغات والتخطيط",
                subtitle = "٩ لغات، QWERTY/AZERTY/Dvorak، ١١ عموداً، ومحرر المفاتيح",
                icon = Icons.Filled.Language,
                route = RouteLanguagesAndLayout,
            ),
            DashboardSectionItem(
                id = "dimensions",
                title = "الأبعاد واليد الواحدة",
                subtitle = "الارتفاع (٥٠٪–١٥٠٪)، عرض الأزرار، الزوايا، والوضع العائم",
                icon = Icons.Filled.AspectRatio,
                route = RouteDimensions,
            ),
            DashboardSectionItem(
                id = "themes",
                title = "السمات والخلفيات",
                subtitle = "آيفون، جبورد، Material You، AMOLED، وصور المعرض",
                icon = Icons.Filled.Palette,
                route = RouteThemes,
            ),
            DashboardSectionItem(
                id = "bars",
                title = "الأشرطة وإيماءات المسطرة",
                subtitle = "السحب العمودي على المسطرة، نمط الإيموجي، والأرقام العربية",
                icon = Icons.Filled.SwipeUp,
                route = RouteBarsAndGestures,
            ),
            DashboardSectionItem(
                id = "clipboard",
                title = "الحافظة والأمان",
                subtitle = "قفل البصمة، مدة الاحتفاظ، القوالب الجاهزة، والعناصر المثبّتة",
                icon = Icons.Filled.ContentPaste,
                route = RouteClipboard,
            ),
            DashboardSectionItem(
                id = "translation",
                title = "الترجمة دون اتصال",
                subtitle = "إدارة حزم لغات ML Kit والترجمة السحابية",
                icon = Icons.Filled.Translate,
                route = RouteTranslation,
            ),
            DashboardSectionItem(
                id = "decorate",
                title = "الزخرفة والمفضلة",
                subtitle = "إدارة الأنماط والإطارات المزخرفة المحفوظة",
                icon = Icons.Filled.AutoAwesome,
                route = RouteDecorate,
            ),
            DashboardSectionItem(
                id = "feedback",
                title = "الاهتزاز والصوت والتصحيح",
                subtitle = "شدة الاهتزاز، الصوت، معاينة الحرف، والتصحيح التلقائي",
                icon = Icons.Filled.Vibration,
                route = RouteFeedback,
            ),
            DashboardSectionItem(
                id = "backup",
                title = "النسخ الاحتياطي والاستعادة",
                subtitle = "تصدير واستعادة الإعدادات والقاموس والحافظة المثبّتة",
                icon = Icons.Filled.Backup,
                route = RouteBackup,
            ),
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val isHome = currentRoute.contains("RouteHome") || currentRoute.isEmpty()

    val screenTitle = when {
        currentRoute.contains("RouteLanguagesAndLayout") -> "اللغات والتخطيط"
        currentRoute.contains("RouteCustomLayoutEditor") -> "محرر التخطيط المخصص"
        currentRoute.contains("RouteDimensions") -> "الأبعاد والأوضاع"
        currentRoute.contains("RouteThemes") -> "السمات والخلفيات"
        currentRoute.contains("RouteBarsAndGestures") -> "الأشرطة وإيماءات المسطرة"
        currentRoute.contains("RouteClipboard") -> "الحافظة والأمان"
        currentRoute.contains("RouteTranslation") -> "الترجمة وحزم اللغات"
        currentRoute.contains("RouteDecorate") -> "الزخرفة والمفضلة"
        currentRoute.contains("RouteFeedback") -> "الاهتزاز والصوت والتصحيح"
        currentRoute.contains("RouteBackup") -> "النسخ الاحتياطي"
        else -> stringResource(R.string.app_name)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(screenTitle) },
                    navigationIcon = {
                        if (!isHome) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteHome,
                    ) {
                        composable<RouteHome> {
                            HomeAndSetupScreen(
                                settings = settings,
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                sections = sections,
                            )
                        }
                        composable<RouteLanguagesAndLayout> {
                            LanguagesAndLayoutScreen(
                                settings = settings,
                                viewModel = viewModel,
                                onOpenCustomEditor = { lang ->
                                    navController.navigate(RouteCustomLayoutEditor(lang.name))
                                },
                            )
                        }
                        composable<RouteCustomLayoutEditor> { entry ->
                            val args = entry.toRoute<RouteCustomLayoutEditor>()
                            val lang = runCatching { LanguageId.valueOf(args.languageName) }.getOrDefault(LanguageId.ARABIC)
                            CustomLayoutEditorScreen(
                                languageId = lang,
                                settings = settings,
                                viewModel = viewModel,
                            )
                        }
                        composable<RouteDimensions> {
                            DimensionsScreen(settings = settings, viewModel = viewModel)
                        }
                        composable<RouteThemes> {
                            ThemesScreen(settings = settings, viewModel = viewModel)
                        }
                        composable<RouteBarsAndGestures> {
                            BarsAndGesturesScreen(settings = settings, viewModel = viewModel)
                        }
                        composable<RouteClipboard> {
                            ClipboardSettingsScreen(settings = settings, viewModel = viewModel)
                        }
                        composable<RouteTranslation> {
                            TranslationSettingsScreen(settings = settings, viewModel = viewModel)
                        }
                        composable<RouteDecorate> {
                            DecorateAndFeedbackScreen(settings = settings, viewModel = viewModel, sectionMode = "decorate")
                        }
                        composable<RouteFeedback> {
                            DecorateAndFeedbackScreen(settings = settings, viewModel = viewModel, sectionMode = "feedback")
                        }
                        composable<RouteBackup> {
                            DecorateAndFeedbackScreen(settings = settings, viewModel = viewModel, sectionMode = "backup")
                        }
                    }
                }
            }
        }
    }
}
