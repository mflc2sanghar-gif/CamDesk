package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object DocumentsRoute

@Serializable
object FoldersRoute

@Serializable
object ToolsRoute

@Serializable
object SettingsRoute

@Serializable
object ScannerRoute

@Serializable
data class CropRoute(val imageUri: String)
