package group.two.knockledge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.ui.LiveTestAppShell
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Single-activity Material 3 live test UI shell entrypoint.
 * Hosts [LiveTestAppShell] and handles [Manifest.permission.RECORD_AUDIO] and
 * [Manifest.permission.POST_NOTIFICATIONS] permissions.
 */
class MainActivity : ComponentActivity() {
    private var knockDetector: KnockGestureDetector? = null
    private var hasRecordAudioPermission by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO]
            ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        hasRecordAudioPermission = audioGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

        hasRecordAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        val requestMissingPermissions = {
            val ungranted = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (ungranted.isNotEmpty()) {
                requestPermissionsLauncher.launch(ungranted)
            }
        }

        requestMissingPermissions()

        val detector = AndroidKnockGestureDetector(application)
        knockDetector = detector
        detector.startListening {
            android.util.Log.i("KnockTest", "KNOCK DETECTED LIVE")
        }

        setContent {
            KnockledgeTheme {
                val app = application as KnockledgeApplication
                LiveTestAppShell(
                    component = app.component,
                    hasRecordAudioPermission = hasRecordAudioPermission,
                    onRequestPermission = requestMissingPermissions,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        knockDetector?.stopListening()
        knockDetector = null
    }
}
