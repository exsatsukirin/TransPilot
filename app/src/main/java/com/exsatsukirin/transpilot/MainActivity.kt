package com.exsatsukirin.transpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exsatsukirin.transpilot.ui.*
import com.exsatsukirin.transpilot.ui.theme.TransPilotTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TranslatorViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            TransPilotTheme(themeMode = themeMode) {
                var selectedTab by remember { mutableStateOf(0) }

                val tabs = listOf("翻译", "历史", "设置")
                val icons = listOf(
                    Icons.Default.Translate,
                    Icons.Default.History,
                    Icons.Default.Settings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("TransPilot") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, label ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(icons[index], contentDescription = label)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> TranslateScreen(viewModel)
                            1 -> HistoryScreen(viewModel)
                            2 -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
