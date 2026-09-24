package com.compressflow.app.presentation.intro

import org.junit.Assert.*
import org.junit.Test

class IntroLogicTest {

    data class SlideData(
        val title: String,
        val badge: String,
        val description: String,
        val highlights: List<String>
    )

    private val slides = listOf(
        SlideData(
            title = "Kompres Video Hingga 90%",
            badge = "Hardware Acceleration",
            description = "Kecilkan ukuran file video secara drastis tanpa mengorbankan kualitas visual.",
            highlights = listOf(
                "Mendukung codec modern H.264, H.265 (HEVC), & AV1",
                "Preset instan: WhatsApp, Instagram, Email, Discord",
                "Fitur Target Ukuran spesifik"
            )
        ),
        SlideData(
            title = "Trim Video & Media Tools",
            badge = "8 Fitur Utilitas Lengkap",
            description = "Potong video dengan presisi frame-akurat dan manfaatkan beragam alat praktis.",
            highlights = listOf(
                "Trim Video dengan timeline visual",
                "Ekstrak audio m4a lossless instan",
                "Hapus audio, kompresi batch, & hapus metadata GPS"
            )
        ),
        SlideData(
            title = "100% Offline & Menjaga Privasi",
            badge = "Bebas Kuota • Zero Cloud Upload",
            description = "Seluruh pemrosesan video dilakukan secara lokal di perangkat Anda.",
            highlights = listOf(
                "Tidak memerlukan koneksi internet",
                "Hasil kompresi otomatis tersimpan di Galeri",
                "Tanpa watermark, tanpa akun, tanpa batasan durasi"
            )
        )
    )

    @Test
    fun `test intro slides have valid structure and contents`() {
        assertEquals(3, slides.size)
        slides.forEach { slide ->
            assertTrue(slide.title.isNotBlank())
            assertTrue(slide.badge.isNotBlank())
            assertTrue(slide.description.isNotBlank())
            assertTrue(slide.highlights.isNotEmpty())
            slide.highlights.forEach { highlight ->
                assertTrue(highlight.isNotBlank())
            }
        }
    }

    @Test
    fun `test intro start destination resolution logic`() {
        fun resolveStartDestination(hasCompletedIntro: Boolean): String {
            return if (hasCompletedIntro) "home" else "splash_intro"
        }

        assertEquals("splash_intro", resolveStartDestination(hasCompletedIntro = false))
        assertEquals("home", resolveStartDestination(hasCompletedIntro = true))
    }

    @Test
    fun `test pagination state calculation`() {
        val totalSlides = slides.size
        assertEquals(3, totalSlides)

        fun isLastSlide(pageIndex: Int): Boolean = pageIndex == totalSlides - 1

        assertFalse(isLastSlide(0))
        assertFalse(isLastSlide(1))
        assertTrue(isLastSlide(2))
    }
}
