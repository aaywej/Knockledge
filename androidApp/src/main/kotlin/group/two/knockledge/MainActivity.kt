package group.two.knockledge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.ui.theme.KnockledgeTheme

class MainActivity : ComponentActivity() {
    private var knockDetector: KnockGestureDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val detector = AndroidKnockGestureDetector(application)
        knockDetector = detector
        detector.startListening {
            android.util.Log.i("KnockTest", "KNOCK DETECTED LIVE")
        }

        setContent {
            KnockledgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Knock Gesture Detector Active\nKnock twice on the phone body or surface to test.",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        knockDetector?.stopListening()
        knockDetector = null
    }
}
