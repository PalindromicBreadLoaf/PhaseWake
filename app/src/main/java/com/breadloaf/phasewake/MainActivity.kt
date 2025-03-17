package com.breadloaf.phasewake

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlarmApp()
        }
    }
}

@Composable
fun AlarmApp() {
    val context = LocalContext.current
    var latestWakeTime by remember { mutableStateOf(Calendar.getInstance()) }
    var optimalWakeTime by remember { mutableStateOf<Calendar?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Select Latest Wake-Up Time", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showTimePicker = true }) {
            Text(text = "Pick Time")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Latest Wake-Up: ${formatTime(latestWakeTime)}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { optimalWakeTime = calculateBestWakeTime(latestWakeTime) }) {
            Text(text = "Going to Bed")
        }

        Spacer(modifier = Modifier.height(16.dp))

        optimalWakeTime?.let {
            Text(text = "Optimal Wake-Up: ${formatTime(it)}")
        }
    }

    if (showTimePicker) {
        ShowTimePicker(
            context = context,
            initialTime = latestWakeTime,
            onTimeSelected = { latestWakeTime = it },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun ShowTimePicker(
    context: Context,
    initialTime: Calendar,
    onTimeSelected: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    val currentHour = initialTime.get(Calendar.HOUR_OF_DAY)
    val currentMinute = initialTime.get(Calendar.MINUTE)

    LaunchedEffect(Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val newTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                onTimeSelected(newTime)
            },
            currentHour,
            currentMinute,
            false
        ).show()
        onDismiss()
    }
}

fun calculateBestWakeTime(latestWakeTime: Calendar): Calendar {
    val now = Calendar.getInstance()
    val wakeTime = now.clone() as Calendar

    // If latestWakeTime is earlier in the day than now, it's actually for the next day
    if (latestWakeTime.before(now)) {
        latestWakeTime.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Keep adding 90-minute cycles until we reach or surpass the latest wake-up time
    while (wakeTime.timeInMillis + 90 * 60 * 1000 <= latestWakeTime.timeInMillis) {
        wakeTime.add(Calendar.MINUTE, 90)
    }

    return wakeTime
}


@SuppressLint("DefaultLocale")
fun formatTime(calendar: Calendar): String {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return String.format("%02d:%02d", hour, minute)
}
