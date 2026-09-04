package com.example

import android.net.Uri
import com.example.data.model.Song
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun testSongFormattedDuration() {
    val songShort = Song(
      id = 1L,
      title = "Track 1",
      artist = "Artist",
      album = "Album",
      albumId = 1L,
      durationMs = 215000L, // 3 mins 35 seconds
      contentUri = Uri.parse("content://media/external/audio/media/1"),
      albumArtUri = Uri.parse("content://media/external/audio/albumart/1"),
      dateAdded = 1700000000L,
      size = 5000000L
    )
    assertEquals("3:35", songShort.formattedDuration)

    val songHour = songShort.copy(durationMs = 3665000L) // 1 hr 1 min 5 secs
    assertEquals("1:01:05", songHour.formattedDuration)

    val songZero = songShort.copy(durationMs = 0L)
    assertEquals("0:00", songZero.formattedDuration)
  }
}

