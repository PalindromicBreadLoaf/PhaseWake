package com.breadloaf.phasewake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Toast.makeText(context, "Wake up! ☀️", Toast.LENGTH_LONG).show()

        // Play alarm sound
        val mediaPlayer = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer.start()
    }
}
