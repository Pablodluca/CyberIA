// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.example", appContext.packageName)
  }
}
