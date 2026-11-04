package com.example.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Resolve with the current resource configuration, including in-place locale changes. */
@Composable
fun Message.localized(): String {
    LocalConfiguration.current
    return resolve(LocalContext.current)
}
