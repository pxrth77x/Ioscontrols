package com.example.iosccenter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 96, 48, 48)
        }
        col.addView(TextView(this).apply {
            text = "iOS Control Center\n1) Enable the accessibility service\n2) Grant the permissions\n3) Enable the module in LSPosed (scope: System UI)"
            textSize = 16f
        })
        fun btn(label: String, action: () -> Unit) = col.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })
        btn("Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("Allow modifying system settings") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
        btn("Allow Do Not Disturb access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        btn("Test: show Control Center") {
            sendBroadcast(Intent(ControlCenterService.ACTION_SHOW).setPackage(packageName))
        }
        setContentView(col)
    }
}
