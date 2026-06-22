package com.github.soundpod.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailTest {

    @Test
    fun testYouTubeThumbnails() {
        val url = "https://i.ytimg.com/vi/abc/hqdefault.jpg"
        
        assertEquals("https://i.ytimg.com/vi/abc/default.jpg", url.thumbnail(100))
        assertEquals("https://i.ytimg.com/vi/abc/mqdefault.jpg", url.thumbnail(300))
        assertEquals("https://i.ytimg.com/vi/abc/hqdefault.jpg", url.thumbnail(450))
        assertEquals("https://i.ytimg.com/vi/abc/sddefault.jpg", url.thumbnail(600))
        assertEquals("https://i.ytimg.com/vi/abc/maxresdefault.jpg", url.thumbnail(800))
    }

    @Test
    fun testGoogleUserContentThumbnails() {
        val url = "https://lh3.googleusercontent.com/abc=w120-h120"
        assertEquals("https://lh3.googleusercontent.com/abc=w500-h500-p-l100-rj", url.thumbnail(500))
    }

    @Test
    fun testGgphtThumbnails() {
        val url = "https://yt3.ggpht.com/abc=s88-c-k-c0x00ffffff-no-rj"
        assertEquals("https://yt3.ggpht.com/abc=w500-h500-p-l100-rj", url.thumbnail(500))
    }
}
