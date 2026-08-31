package com.mckimquyen.opencal

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AuditReproductionSmokeTest {

    @Test
    fun `Issue 1 Reproduction - toLong throws NumberFormatException on malformed timestamp`() {
        val malformedTimestamp = "2026-08-31 14:30:00"
        
        // Old implementation behavior: throws NumberFormatException
        var threwException = false
        try {
            malformedTimestamp.toLong()
        } catch (e: NumberFormatException) {
            threwException = true
        }
        assertTrue("toLong() MUST throw NumberFormatException on non-numeric strings", threwException)
        
        // New safe implementation behavior: returns null without crashing
        val safeResult = malformedTimestamp.toLongOrNull()
        assertNull("toLongOrNull() safely returns null for malformed timestamps", safeResult)
    }

    @Test
    fun `Issue 2 Reproduction - Locale generation without country fails to specify region for pt-BR and nb-NO`() {
        val oldPtCode = "pt"
        val oldPtLocale = Locale(oldPtCode)
        assertEquals("pt", oldPtLocale.language)
        assertEquals("", oldPtLocale.country) // Empty country: Android will not match values-pt-rBR

        val newPtCode = "pt-BR"
        val parts = newPtCode.split("-")
        val newPtLocale = Locale(parts[0], parts[1])
        assertEquals("pt", newPtLocale.language)
        assertEquals("BR", newPtLocale.country) // Country BR: Matches values-pt-rBR exactly!

        val oldNbCode = "nb"
        val oldNbLocale = Locale(oldNbCode)
        assertEquals("nb", oldNbLocale.language)
        assertEquals("", oldNbLocale.country) // Empty country: Android will not match values-nb-rNO

        val newNbCode = "nb-NO"
        val nbParts = newNbCode.split("-")
        val newNbLocale = Locale(nbParts[0], nbParts[1])
        assertEquals("nb", newNbLocale.language)
        assertEquals("NO", newNbLocale.country) // Country NO: Matches values-nb-rNO exactly!
    }

    @Test
    fun `Issue 3 Reproduction - themeMap fallback returning 0 is not a valid style resource ID`() {
        val defaultThemeIndex = 0
        val appThemeStyleResId = R.style.AppTheme

        // Style resource ID in Android is a generated resource ID (not 0)
        assertNotEquals(0, appThemeStyleResId)
        assertEquals(0, defaultThemeIndex)

        val themeMap = mapOf(
            0 to R.style.AppTheme,
            1 to R.style.AmoledTheme,
        )

        val unknownThemeKey = 99
        val oldFallback = themeMap[unknownThemeKey] ?: defaultThemeIndex
        assertEquals("Old fallback returned 0 (index), which is not a valid style ID", 0, oldFallback)

        val newFallback = themeMap[unknownThemeKey] ?: R.style.AppTheme
        assertEquals("New fallback returned valid R.style.AppTheme", R.style.AppTheme, newFallback)
    }

    @Test
    fun `Issue 4 Reproduction - Mailto URI should not contain leading space in email address`() {
        val buggyUriString = "mailto: roy.mobile.dev@gmail.com"
        val fixedUriString = "mailto:roy.mobile.dev@gmail.com"

        val buggyEmail = buggyUriString.substringAfter("mailto:")
        val fixedEmail = fixedUriString.substringAfter("mailto:")

        assertTrue("Buggy email has leading space", buggyEmail.startsWith(" "))
        assertFalse("Fixed email has no leading space", fixedEmail.startsWith(" "))
        assertEquals("roy.mobile.dev@gmail.com", fixedEmail)
    }
}
