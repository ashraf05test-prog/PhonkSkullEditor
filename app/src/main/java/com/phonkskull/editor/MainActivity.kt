package com.phonkskull.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.phonkskull.editor.ui.AccentPurple
import com.phonkskull.editor.ui.BgDark
import com.phonkskull.editor.ui.MainScreen
import com.phonkskull.editor.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.init(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentPurple,
                    background = BgDark,
                    surface = Color(0xFF12121E),
                )
            ) {
                MainScreen(vm)
            }
        }
    }
}
