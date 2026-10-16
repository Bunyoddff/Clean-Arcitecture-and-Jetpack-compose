package uz.ttpu.framephases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Switch between demos to test each question:
                    LectureTree()           // Question 4
                    // PaddingDemo()        // Question 5
                    // OffsetDemo()         // Question 6
                    // ColorDemo()          // Question 7
                    // ParallaxSlow()       // Question 9 (slow)
                    // ParallaxFast()       // Question 9 (fast)
                    // ScrollToTopNaive()   // Question 10 (naive)
                    // ScrollToTopOptimized()// Question 10 (derivedStateOf)
                    // Parent()             // Question 11
                    // CyclicDemo()         // Question 13 (cyclic)
                    // CyclicFixed()        // Question 13 (fixed)
                    // StackBelowDemo()     // Question 14 (custom layout)
                }
            }
        }
    }
}
