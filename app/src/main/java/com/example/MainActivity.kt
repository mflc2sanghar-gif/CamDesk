package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.documents.DocumentsScreen
import com.example.ui.folders.FoldersScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.CropRoute
import com.example.ui.navigation.DocumentsRoute
import com.example.ui.navigation.FoldersRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.ScannerRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.ToolsRoute
import com.example.ui.scanner.CropScreen
import com.example.ui.scanner.ScannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CamDeskTheme
import com.example.ui.tools.ToolsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamDeskTheme {
                CamDeskApp()
            }
        }
    }
}

@Composable
fun CamDeskApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        Triple("Home", Icons.Filled.Home to Icons.Outlined.Home, HomeRoute),
        Triple("Docs", Icons.Filled.Description to Icons.Outlined.Description, DocumentsRoute),
        Triple("Folders", Icons.Filled.Folder to Icons.Outlined.Folder, FoldersRoute),
        Triple("Tools", Icons.Filled.Build to Icons.Outlined.Build, ToolsRoute),
        Triple("Settings", Icons.Filled.Settings to Icons.Outlined.Settings, SettingsRoute)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                navItems.forEach { (label, icons, route) ->
                    val isSelected = currentDestination?.hierarchy?.any { 
                        it.route?.contains(route::class.qualifiedName ?: "") == true 
                    } == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) icons.first else icons.second,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> { 
                HomeScreen(
                    onScanClick = { navController.navigate(ScannerRoute) }
                ) 
            }
            composable<DocumentsRoute> { DocumentsScreen() }
            composable<FoldersRoute> { FoldersScreen() }
            composable<ToolsRoute> { ToolsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ScannerRoute> {
                ScannerScreen(
                    onImageCaptured = { uri -> 
                        navController.navigate(CropRoute(imageUri = uri.toString())) {
                            popUpTo(ScannerRoute) { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() }
                )
            }
            composable<CropRoute> { backStackEntry ->
                val cropRoute = backStackEntry.toRoute<CropRoute>()
                CropScreen(
                    imageUri = cropRoute.imageUri,
                    onCropComplete = { croppedUri ->
                        // Navigate back or to a document editor/saver view
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}

