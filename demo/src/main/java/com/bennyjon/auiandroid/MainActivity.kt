package com.bennyjon.auiandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.auiandroid.livechat.DemoAuiTheme
import com.bennyjon.auiandroid.livechat.LiveChatScreen
import com.bennyjon.auiandroid.livechat.LiveChatViewModel
import com.bennyjon.auiandroid.settings.SettingsScreen
import com.bennyjon.auiandroid.settings.SystemPromptScreen
import com.bennyjon.auiandroid.settings.SystemPromptViewModel
import com.bennyjon.auiandroid.showcase.ShowcaseScreen
import com.bennyjon.auiandroid.showcase.ShowcaseViewModel
import com.bennyjon.auiandroid.ui.theme.AUIAndroidTheme
import com.bennyjon.auiandroid.ui.theme.DemoThemes
import com.bennyjon.auiandroid.ui.theme.green.GreenTheme
import com.bennyjon.auiandroid.ui.theme.toMaterialColorScheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmTheme
import dagger.hilt.android.AndroidEntryPoint

private const val HomeRoute = "home"

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

    val vm: LiveChatViewModel = hiltViewModel()
    val selectedTheme by vm.selectedTheme.collectAsState()
    val auiTheme = when (selectedTheme) {
        DemoAuiTheme.DEFAULT -> AuiTheme.fromMaterialTheme()
        DemoAuiTheme.WARM_ORGANIC -> DemoThemes.warmOrganic()
        DemoAuiTheme.EARTHY_GREEN -> DemoThemes.earthyGreen()
    }
    val typography = when (selectedTheme) {
        DemoAuiTheme.DEFAULT -> MaterialTheme.typography
        DemoAuiTheme.WARM_ORGANIC -> WarmTheme.WarmTypography
        DemoAuiTheme.EARTHY_GREEN -> GreenTheme.GreenTypography
    }
    val colorScheme = auiTheme.colors.toMaterialColorScheme(
        base = MaterialTheme.colorScheme,
    )

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable(HomeRoute) {
            DemoHomeScreen(
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route)
                },
            )
        }
        composable(DemoDestination.LIVE_CHAT.route) {
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                LiveChatScreen(
                    viewModel = vm,
                    pluginRegistry = vm.pluginRegistry,
                    theme = selectedTheme,
                    onChangeTheme = { vm.switchTheme(it) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(DemoDestination.SHOWCASE.route) {
            val showcaseVm: ShowcaseViewModel = hiltViewModel()
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                ShowcaseScreen(
                    viewModel = showcaseVm,
                    pluginRegistry = showcaseVm.pluginRegistry,
                    auiTheme = auiTheme,
                    selectedThemeName = selectedTheme,
                    onChangeTheme = { vm.switchTheme(it) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(DemoDestination.SETTINGS.route) {
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSystemPrompt = {
                        navController.navigate(DemoDestination.SYSTEM_PROMPT.route)
                    },
                )
            }
        }
        composable(DemoDestination.SYSTEM_PROMPT.route) {
            val systemPromptVm: SystemPromptViewModel = hiltViewModel()
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                SystemPromptScreen(
                    generatedPrompt = systemPromptVm.generatedPrompt,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        themeChatDestinations.forEach { destination ->
            composable(destination.route) {
                val (title, auiTheme) = resolveTheme(destination)
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
private fun resolveTheme(destination: DemoDestination): Pair<String, AuiTheme> = when (destination) {
    DemoDestination.WARM_ORGANIC_CHAT -> destination.homeTitle to DemoThemes.warmOrganic()
    DemoDestination.EARTHY_GREEN_CHAT -> destination.homeTitle to DemoThemes.earthyGreen()
    else -> "Default" to AuiTheme.fromMaterialTheme()
}

private val themeChatDestinations = listOf(
    DemoDestination.WARM_ORGANIC_CHAT,
    DemoDestination.EARTHY_GREEN_CHAT,
)
