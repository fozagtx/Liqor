package com.liqor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.liqor.ui.theme.CautionText
import com.liqor.ui.theme.CautionTint
import com.liqor.ui.theme.DangerText
import com.liqor.ui.theme.DangerTint
import com.liqor.ui.theme.SafeText
import com.liqor.ui.theme.SafeTint
import com.liqor.ui.theme.StaleText
import com.liqor.ui.theme.StaleTint
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyAndColorTest {
    @Test
    fun stringsHaveNoForbiddenGlyphOrClaims() {
        val paths = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml"),
        )
        val xml = paths.firstOrNull { it.exists() }?.readText()
            ?: throw AssertionError("strings.xml was not found")
        val forbidden = listOf(
            "protects",
            "never get liquidated",
            "wakes you up",
            "real-time",
        )

        assertFalse(xml.contains('\u2014'))
        forbidden.forEach { phrase ->
            assertFalse("Forbidden phrase: $phrase", xml.contains(phrase, ignoreCase = true))
        }
    }

    @Test
    fun chipTextContrastExceedsFourPointFiveToOne() {
        val chips = listOf(
            SafeText to SafeTint,
            CautionText to CautionTint,
            DangerText to DangerTint,
            StaleText to StaleTint,
        )

        chips.forEach { (foreground, background) ->
            assertTrue(contrastRatio(foreground, background) >= 4.5)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val luminance = listOf(first.luminance(), second.luminance()).sorted()
        return (luminance[1] + 0.05) / (luminance[0] + 0.05)
    }
}
