/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import me.him188.ani.app.data.models.preference.DebugSettings
import me.him188.ani.app.data.repository.user.SettingsRepository
import me.him188.ani.utils.platform.annotations.TestOnly
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class DebugSettingsViewModel : AbstractViewModel(), KoinComponent {
    private val settingsRepository by inject<SettingsRepository>()
    val debugSettings =
        settingsRepository.debugSettings.flow.produceState(DebugSettings(_placeHolder = -1))

    suspend fun updateDebugSettings(settings: DebugSettings) {
        settingsRepository.debugSettings.set(settings)
    }

    @TestOnly
    var isAppInDebugModeOverride by mutableStateOf(false)

    @OptIn(TestOnly::class)
    val isAppInDebugMode: Boolean by derivedStateOf {
        isAppInDebugModeOverride || debugSettings.value.enabled
    }
}

@Composable
fun isInDebugMode(): Boolean {
    val vm = rememberDebugSettingsViewModel()
    return vm.isAppInDebugMode
}

@Composable
fun rememberDebugSettingsViewModel(): DebugSettingsViewModel =
    viewModel<DebugSettingsViewModel> { DebugSettingsViewModel() }