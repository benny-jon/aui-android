package com.bennyjon.auiandroid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.AuiCatalogPrompt
import com.bennyjon.auiandroid.livechat.DemoAuiTheme
import com.bennyjon.auiandroid.livechat.LiveChatScreen
import com.bennyjon.auiandroid.livechat.LiveChatViewModel
import com.bennyjon.auiandroid.plugins.DemoPluginRegistry
import com.bennyjon.auiandroid.ui.theme.AUIAndroidTheme
import com.bennyjon.auiandroid.ui.theme.DemoThemes
import com.bennyjon.auiandroid.ui.theme.toMaterialColorScheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AUIAndroidTheme {
                DemoNavHost()
            }
        }
    }
}

@Composable
private fun DemoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            DemoHomeScreen(
                onThemeSelected = { themeKey ->
                    if (themeKey == "live_chat") {
                        navController.navigate("live_chat")
                    } else {
                        navController.navigate("chat/$themeKey")
                    }
                },
            )
        }
        composable("live_chat") {
            val vm: LiveChatViewModel = hiltViewModel()
            val selectedTheme by vm.selectedTheme.collectAsState()
            val auiTheme = when (selectedTheme) {
                DemoAuiTheme.DEFAULT -> AuiTheme.fromMaterialTheme()
                DemoAuiTheme.WARM_ORGANIC -> DemoThemes.warmOrganic()
                DemoAuiTheme.EARTHY_GREEN -> DemoThemes.earthyGreen()
            }
            val colorScheme = auiTheme.colors.toMaterialColorScheme(
                base = MaterialTheme.colorScheme,
            )
            MaterialTheme(colorScheme = colorScheme) {
                LiveChatScreen(
                    viewModel = vm,
                    pluginRegistry = vm.pluginRegistry,
                    theme = selectedTheme,
                    onChangeTheme = { vm.switchTheme(it) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable("chat/{theme}") { backStackEntry ->
            val themeKey = backStackEntry.arguments?.getString("theme") ?: "default"

            if (themeKey == "plugins") {
                val context = LocalContext.current
                val pluginRegistry = DemoPluginRegistry.create(context)

                LaunchedEffect(Unit) {
                    val prompt = AuiCatalogPrompt.generate(pluginRegistry)
                    Log.d("AuiDemo", "AuiCatalogPrompt with plugins:\n$prompt")
                }

                val vm: DemoViewModel = viewModel(
                    key = "plugins",
                    factory = DemoViewModelFactory(DemoViewModel.PLUGIN_SEQUENCE),
                )

                ChatScreen(
                    viewModel = vm,
                    title = "Plugin Showcase",
                    auiTheme = AuiTheme.fromMaterialTheme(),
                    pluginRegistry = pluginRegistry,
                    onBack = { navController.popBackStack() },
                )
            } else if (themeKey == "all_components") {
                val context = LocalContext.current
                val pluginRegistry = DemoPluginRegistry.create(context)

                val vm: DemoViewModel = viewModel(
                    key = "complex_demos",
                    factory = DemoViewModelFactory(DemoViewModel.FULL_DEMO_SEQUENCE),
                )

                ChatScreen(
                    viewModel = vm,
                    title = "All Components Demo",
                    auiTheme = AuiTheme.fromMaterialTheme(),
                    pluginRegistry = pluginRegistry,
                    onBack = { navController.popBackStack() },
                )
            } else {
                val (title, auiTheme) = resolveTheme(themeKey)
                val vm: DemoViewModel = viewModel()

                ChatScreen(
                    viewModel = vm,
                    title = title,
                    auiTheme = auiTheme,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun resolveTheme(key: String): Pair<String, AuiTheme> = when (key) {
    "warm_organic" -> "Warm Organic" to DemoThemes.warmOrganic()
    "earthy_green" -> "Earthy Green" to DemoThemes.earthyGreen()
    else -> "Default" to AuiTheme.fromMaterialTheme()
}
