package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StickyDesk AI", appName)
  }

  @Test
  fun `activity launches without crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `widget provider broadcast executes cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.widget.TodayTasksWidgetProvider.sendUpdateBroadcast(context)
    val widgetInfo = context.resources.getXml(R.xml.today_tasks_widget_info)
    org.junit.Assert.assertNotNull(widgetInfo)
  }

  @Test
  fun `manual api key manager stores and retrieves key correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ai.ApiKeyManager.setManualApiKey(context, "AIzaSyTestKey12345")
    val retrieved = com.example.ai.ApiKeyManager.getManualApiKey(context)
    assertEquals("AIzaSyTestKey12345", retrieved)
    assertEquals(com.example.ai.KeySource.MANUAL, com.example.ai.ApiKeyManager.getKeySource(context))
    com.example.ai.ApiKeyManager.clearManualApiKey(context)
    assertEquals("", com.example.ai.ApiKeyManager.getManualApiKey(context))
  }
}
