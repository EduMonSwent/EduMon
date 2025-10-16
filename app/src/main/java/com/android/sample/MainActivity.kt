package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.android.sample.navigation.EduMonNavHost
import com.android.sample.ui.theme.EduMonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EduMonTheme { EduMonNavHost() } } }}
