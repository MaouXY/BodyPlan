package com.maou.bodyplanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.maou.bodyplanapp.ui.BodyPlanRoot
import com.maou.bodyplanapp.ui.theme.BodyPlanAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyPlanAppTheme {
                BodyPlanRoot()
            }
        }
    }
}
