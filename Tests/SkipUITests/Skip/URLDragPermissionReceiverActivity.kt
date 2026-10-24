// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button as AndroidButton
import androidx.compose.material3.Text as AndroidText
import androidx.compose.runtime.mutableStateOf
import java.security.MessageDigest

/**
 * Instrumentation-APK-only receiver. Launch beside the production app in split screen, drag the
 * app's prepared PNG here, then press Read. The delayed read occurs after the SDK drop callback
 * and drag session have ended. Compare bytes/hash to the sender; a same-UID round trip cannot
 * establish that Android granted cross-app provider access. No directories/providers are exposed.
 */
class URLDragPermissionReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pending = mutableStateOf<Uri?>(null)
        val status = mutableStateOf("Waiting for a drop from a different UID")
        val reading = mutableStateOf(false)
        Log.i("SkipUIDragGrant", "receiverStarted package=$packageName uid=${Process.myUid()}")
        setContent {
            Column {
                URLDragPermissionReceiverProbe { url ->
                    pending.value = Uri.parse(url.absoluteString)
                    status.value = "Drop received. End the drag, then press Read."
                }.Compose(ComposeContext())
                AndroidText(status.value)
                AndroidButton(enabled = pending.value != null && !reading.value, onClick = {
                    val uri = pending.value ?: return@AndroidButton
                    reading.value = true
                    status.value = "Reading after callback..."
                    Thread {
                        try {
                            Thread.sleep(1000)
                            val readGrant = checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            check(readGrant == PackageManager.PERMISSION_GRANTED) { "No read grant" }
                            val digest = MessageDigest.getInstance("SHA-256")
                            var total = 0L
                            contentResolver.openInputStream(uri)!!.use { input ->
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    check(total <= 64L * 1024 * 1024) { "Test read exceeds 64 MiB" }
                                    digest.update(buffer, 0, count)
                                }
                            }
                            val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                            val evidence = "uid=${Process.myUid()} authority=${uri.authority} readGrant=$readGrant bytes=$total sha256=$hash"
                            Log.i("SkipUIDragGrant", "delayedReadSuccess $evidence")
                            runOnUiThread { if (!isDestroyed) { status.value = "Read OK: $total bytes SHA256 $hash"; reading.value = false } }
                        } catch (error: Exception) {
                            Log.e("SkipUIDragGrant", "delayedReadFailed uid=${Process.myUid()} ${error.javaClass.simpleName}: ${error.message}")
                            runOnUiThread { if (!isDestroyed) { status.value = "Read failed: ${error.javaClass.simpleName}"; reading.value = false } }
                        }
                    }.start()
                }) { AndroidText("Read dropped URI after callback") }
            }
        }
    }
}
