package group.two.knockledge

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import group.two.knockledge.di.DaggerKnockledgeComponent
import group.two.knockledge.di.KnockledgeComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the process-wide dependency graph and installs App Check before any Firebase call.
 */
class KnockledgeApplication : Application() {

    /** Dagger graph root, built once in [onCreate]. */
    lateinit var component: KnockledgeComponent
        private set

    /** One graph per process, shared by MainActivity and KnockledgeService. */
    lateinit var graph: KnockledgeGraph
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Installs App Check (debug secret in debug builds, Play Integrity in release), builds the
     * component, then syncs queued notes whenever the network comes back.
     */
    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Reinstalls wipe SharedPreferences, regenerating the debug secret; pin one instead.
                if (BuildConfig.APP_CHECK_DEBUG_SECRET.isNotEmpty()) {
                    getSharedPreferences(
                        "com.google.firebase.appcheck.debug.store.${FirebaseApp.getInstance().persistenceKey}",
                        MODE_PRIVATE,
                    ).edit().putString("com.google.firebase.appcheck.debug.DEBUG_SECRET",
                        BuildConfig.APP_CHECK_DEBUG_SECRET).apply()
                }
                DebugAppCheckProviderFactory.getInstance()
            } else PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
        component = DaggerKnockledgeComponent.factory().create(this)
        graph = component.graph()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val uid = component.data().auth.currentUser.value?.uid ?: return
                    scope.launch { component.data().notes.syncPending(uid) }
                }
            },
        )
    }
}
