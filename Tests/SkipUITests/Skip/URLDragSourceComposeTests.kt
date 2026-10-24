// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.View as AndroidView
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import skip.foundation.URL
import java.io.File
import java.io.FileNotFoundException

/** A narrow, test-only provider. It does not export any host directories or guess app policy. */
class DragSourceTestProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri): String = "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r" || uri.path != "/figure.png") throw FileNotFoundException()
        val file = File(requireNotNull(context).cacheDir, "sdk-drag-test.png")
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class URLDragSourceComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun contentURITransferKeepsMIMEAndOnlyGrantsGlobalReadAccess() {
        val context = rule.activity
        val value = "content://${context.packageName}.drag-source-test/figure.png"
        val uri = Uri.parse(value)
        val transfer = urlDragTransferData(context, URL(string = value)!!)
        assertNotNull(transfer)
        transfer!!
        assertEquals(1, transfer.clipData.itemCount)
        assertEquals(uri, transfer.clipData.getItemAt(0).uri)
        assertTrue(transfer.clipData.description.hasMimeType("image/png"))
        assertEquals(AndroidView.DRAG_FLAG_GLOBAL or AndroidView.DRAG_FLAG_GLOBAL_URI_READ, transfer.flags)
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertTrue(bytes.contentEquals(byteArrayOf(1, 2, 3, 4)))
        rule.setContent { URLDragSourceProbe(URL(string = value)!!).Compose(ComposeContext()) }
        rule.onNodeWithTag("url-drag-source").assertIsDisplayed()
    }

    /** Real Android drag dispatcher, not helper extraction; Robolectric has no OS drag session. */
    @Test fun actualLongPressDragReachesTheDropDestination() {
        assumeFalse("Requires Android OS drag dispatcher", Build.FINGERPRINT.contains("robolectric"))
        val value = "content://${rule.activity.packageName}.drag-source-test/figure.png"
        rule.setContent { URLDragRoundTripProbe(URL(string = value)!!).Compose(ComposeContext()) }
        val sourceBounds = rule.onNodeWithTag("native-drag-source").assertIsDisplayed().fetchSemanticsNode().boundsInWindow
        val targetBounds = rule.onNodeWithTag("native-drag-target").assertIsDisplayed().fetchSemanticsNode().boundsInWindow
        val windowScreenOrigin = rule.runOnIdle {
            val onScreen = IntArray(2)
            val inWindow = IntArray(2)
            rule.activity.window.decorView.getLocationOnScreen(onScreen)
            rule.activity.window.decorView.getLocationInWindow(inWindow)
            Offset((onScreen[0] - inWindow[0]).toFloat(), (onScreen[1] - inWindow[1]).toFloat())
        }
        val source = sourceBounds.center + windowScreenOrigin
        val target = targetBounds.center + windowScreenOrigin
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        var position = source
        fun inject(action: Int) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, position.x, position.y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue("Android OS rejected drag input action $action", automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        // Compose touch injection dispatches directly to a View. A system drag needs the
        // actual OS input stream, including elapsed uptime while the pointer stays down.
        var pointerDown = true
        try {
            inject(MotionEvent.ACTION_DOWN)
            // Pointer-input timeouts use the test scheduler; OS drag input uses real uptime.
            rule.mainClock.advanceTimeByFrame()
            SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 200L)
            rule.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout().toLong() + 200L)
            for (step in 1..12) {
                SystemClock.sleep(25L)
                val fraction = step / 12.0f
                position = source + (target - source) * fraction
                inject(MotionEvent.ACTION_MOVE)
            }
            SystemClock.sleep(100L)
            inject(MotionEvent.ACTION_UP)
            pointerDown = false
        } catch (failure: Throwable) {
            if (pointerDown) {
                try {
                    inject(MotionEvent.ACTION_CANCEL)
                } catch (cleanup: Throwable) {
                    failure.addSuppressed(cleanup)
                }
            }
            throw failure
        }
        rule.onNodeWithText("Dropped 1").assertIsDisplayed()
        android.util.Log.i("SkipUIDragProof", "Actual URL source -> Android OS drag -> dropDestination delivered one URI")
    }

    @Test fun permissionHostIsFoundThroughThemedContextWrappers() {
        val wrapped = android.view.ContextThemeWrapper(android.content.ContextWrapper(rule.activity), android.R.style.Theme_Material_Light)
        org.junit.Assert.assertSame(rule.activity, dragDropActivity(wrapped))
        assertNull(dragDropActivity(rule.activity.applicationContext))
    }

    @Test fun linksAreTransferableWithoutGrantingFilePermissions() {
        val url = "https://example.com/figure.png?x=1#panel"
        val transfer = urlDragTransferData(rule.activity, URL(string = url)!!)!!
        assertEquals(url, transfer.clipData.getItemAt(0).uri.toString())
        assertEquals(AndroidView.DRAG_FLAG_GLOBAL, transfer.flags)
    }

    @Test fun rawPrivatePathsAndUnsupportedSchemesCannotBeAdvertisedAsReadableFiles() {
        for (value in listOf("file:///data/user/0/private/figure.png", "javascript:alert(1)", "relative.png")) {
            assertNull(urlDragTransferData(rule.activity, URL(string = value)!!))
        }
    }
}
