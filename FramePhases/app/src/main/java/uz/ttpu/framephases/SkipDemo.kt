package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun Parent() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { Log.d("Skip", "Parent: composition") }

    Column {
        Button(onClick = { count++ }) { Text("Add") }
        // Defer read to Child using a lambda
        Child(count = { count })
        Sibling()
    }
}

@Composable
fun Child(count: () -> Int) {
    SideEffect { Log.d("Skip", "Child: composition") }
    // State read is executed inside Child's restart scope
    Text("Count: ${count()}")
}

@Composable
fun Sibling() {
    SideEffect { Log.d("Skip", "Sibling: composition") }
    Text("I never change")
}
