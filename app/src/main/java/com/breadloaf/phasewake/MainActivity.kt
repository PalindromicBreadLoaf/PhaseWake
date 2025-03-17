package com.breadloaf.phasewake

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
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

        Button(onClick = {
            if (hasExactAlarmPermission(context)) {
                optimalWakeTime = calculateBestWakeTime(latestWakeTime)

                optimalWakeTime?.let {
                    scheduleAlarm(context, it)
                    saveAlarm(context, it)
                }
            } else {
                requestExactAlarmPermission(context)
            }
        }) {
            Text("Going to Bed")
        }

        Spacer(modifier = Modifier.height(16.dp))

        optimalWakeTime?.let {
            Text(text = "Optimal Wake-Up: ${formatTime(it)}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ShowAlarms(context = context)
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

@Composable
fun ShowAlarms(context: Context) {
    val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    val alarmTimes = sharedPreferences.all.entries.map { it.key }

    LazyColumn {
        items(alarmTimes) { alarmTime ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alarm set for: $alarmTime")
                IconButton(onClick = {
                    removeAlarm(context, alarmTime)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(calendar: Calendar): String {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return String.format("%02d:%02d", hour, minute)
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleAlarm(context: Context, wakeTime: Calendar) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Log.e("AlarmScheduler", "Exact alarm permission is missing!")
        Toast.makeText(context, "Alarm permission is required", Toast.LENGTH_LONG).show()
        return
    }

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime.timeInMillis, pendingIntent)

    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(wakeTime.time)

    Log.d("AlarmScheduler", "Alarm scheduled for: $formattedTime")
    Toast.makeText(context, "Alarm set for $formattedTime", Toast.LENGTH_LONG).show()
}

fun hasExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:" + context.packageName)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Enable 'Exact Alarms' in settings.", Toast.LENGTH_LONG).show()
    }
}

fun saveAlarm(context: Context, wakeTime: Calendar) {
    val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(wakeTime.time)

    editor.putString(formattedTime, wakeTime.timeInMillis.toString())
    editor.apply()
}

fun removeAlarm(context: Context, alarmTime: String) {
    val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val alarmTimeMillis = sharedPreferences.getString(alarmTime, null)?.toLongOrNull()

    if (alarmTimeMillis != null) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)

        sharedPreferences.edit().remove(alarmTime).apply()

        Log.d("AlarmScheduler", "Alarm for $alarmTime removed.")
        Toast.makeText(context, "Alarm for $alarmTime removed.", Toast.LENGTH_SHORT).show()
    }
}
