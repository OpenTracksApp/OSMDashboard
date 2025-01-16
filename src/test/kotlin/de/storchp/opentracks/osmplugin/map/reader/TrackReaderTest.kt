package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.model.Track
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

class TrackReaderTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var cursor: Cursor
    private val testUri: Uri = mockk()

    @BeforeEach
    fun setUp() {
        contentResolver = mockk()
        cursor = mockk()

        every { cursor.getColumnIndexOrThrow(TrackReader.ID) } returns 1
        every { cursor.getColumnIndexOrThrow(TrackReader.NAME) } returns 2
        every { cursor.getColumnIndexOrThrow(TrackReader.DESCRIPTION) } returns 3
        every { cursor.getColumnIndexOrThrow(TrackReader.CATEGORY) } returns 4
        every { cursor.getColumnIndexOrThrow(TrackReader.STARTTIME) } returns 5
        every { cursor.getColumnIndexOrThrow(TrackReader.STOPTIME) } returns 6
        every { cursor.getColumnIndexOrThrow(TrackReader.TOTALDISTANCE) } returns 7
        every { cursor.getColumnIndexOrThrow(TrackReader.TOTALTIME) } returns 8
        every { cursor.getColumnIndexOrThrow(TrackReader.MOVINGTIME) } returns 9
        every { cursor.getColumnIndexOrThrow(TrackReader.AVGSPEED) } returns 10
        every { cursor.getColumnIndexOrThrow(TrackReader.AVGMOVINGSPEED) } returns 11
        every { cursor.getColumnIndexOrThrow(TrackReader.MAXSPEED) } returns 12
        every { cursor.getColumnIndexOrThrow(TrackReader.MINELEVATION) } returns 13
        every { cursor.getColumnIndexOrThrow(TrackReader.MAXELEVATION) } returns 14
        every { cursor.getColumnIndexOrThrow(TrackReader.ELEVATIONGAIN) } returns 15
        every { cursor.close() } just Runs
    }

    @Test
    fun readTracks() {
        var track = createTrack()
        every {
            contentResolver.query(
                testUri,
                TrackReader.PROJECTION,
                null,
                null,
                null
            )
        } returns cursor
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getLong(1) } returnsMany listOf(1, 2)
        every { cursor.getString(2) } returnsMany listOf(track.name)
        every { cursor.getString(3) } returnsMany listOf(track.description)
        every { cursor.getString(4) } returnsMany listOf(track.category)
        every { cursor.getLong(5) } returnsMany listOf(track.startTime!!.toEpochMilli())
        every { cursor.getLong(6) } returnsMany listOf(track.stopTime!!.toEpochMilli())
        every { cursor.getDouble(7) } returnsMany listOf(track.totalDistanceMeter)
        every { cursor.getLong(8) } returnsMany listOf(track.totalTime!!.inWholeMilliseconds)
        every { cursor.getLong(9) } returnsMany listOf(track.movingTime!!.inWholeMilliseconds)
        every { cursor.getDouble(10) } returnsMany listOf(track.avgSpeedMeterPerSecond!!)
        every { cursor.getDouble(11) } returnsMany listOf(track.avgMovingSpeedMeterPerSecond!!)
        every { cursor.getDouble(12) } returnsMany listOf(track.maxSpeedMeterPerSecond!!)
        every { cursor.getDouble(13) } returnsMany listOf(track.minElevationMeter!!)
        every { cursor.getDouble(14) } returnsMany listOf(track.maxElevationMeter!!)
        every { cursor.getDouble(15) } returnsMany listOf(track.elevationGainMeter!!)

        val tracks = TrackReader.readTracks(contentResolver, testUri)

        assertThat(tracks).hasSize(1)
        assertThat(tracks[0]).isEqualTo(track)
    }

    private fun createTrack() = Track(
        id = 1L,
        name = "Test Waypoint",
        description = "Test Description",
        category = "Test Category",
        startTime = Instant.now().minusSeconds(90).truncatedTo(ChronoUnit.MILLIS),
        stopTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
        totalDistanceMeter = 1.0,
        totalTime = 2.seconds,
        movingTime = 1.seconds,
        avgSpeedMeterPerSecond = 1.1,
        avgMovingSpeedMeterPerSecond = 1.2,
        maxSpeedMeterPerSecond = 1.3,
        minElevationMeter = 1.4,
        maxElevationMeter = 1.5,
        elevationGainMeter = 1.6,
    )
}