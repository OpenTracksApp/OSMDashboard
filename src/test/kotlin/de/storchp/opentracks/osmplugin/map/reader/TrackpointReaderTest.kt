package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrackpointReaderTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var cursor: Cursor
    private val testUri: Uri = mockk()

    @BeforeEach
    fun setUp() {
        contentResolver = mockk()
        cursor = mockk()

        every { cursor.getColumnIndexOrThrow(TrackpointReader.ID) } returns 1
        every { cursor.getColumnIndexOrThrow(TrackpointReader.TRACKID) } returns 2
        every { cursor.getColumnIndexOrThrow(TrackpointReader.LATITUDE) } returns 3
        every { cursor.getColumnIndexOrThrow(TrackpointReader.LONGITUDE) } returns 4
        every { cursor.getColumnIndexOrThrow(TrackpointReader.TIME) } returns 5
        every { cursor.getColumnIndexOrThrow(TrackpointReader.SPEED) } returns 6
        every { cursor.close() } just Runs
    }

    @Test
    fun readTracksV1() {
        var trackpoint = createTrackpoint()
        every {
            contentResolver.query(
                testUri,
                TrackpointReader.PROJECTION_V1,
                "${TrackpointReader.ID}> ?",
                arrayOf("1"),
                null
            )
        } returns cursor
        every { cursor.getColumnIndex(TrackpointReader.TYPE) } returns -1
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getLong(1) } returnsMany listOf(2)
        every { cursor.getLong(2) } returnsMany listOf(trackpoint.trackId!!)
        every { cursor.getInt(3) } returnsMany listOf(trackpoint.latLong!!.latitudeE6)
        every { cursor.getInt(4) } returnsMany listOf(trackpoint.latLong.longitudeE6)
        every { cursor.getLong(5) } returnsMany listOf(trackpoint.time!!.toEpochMilli())
        every { cursor.getDouble(6) } returnsMany listOf(trackpoint.speed!!)

        val trackpoints = TrackpointReader.readTrackpointsBySegments(contentResolver, testUri, 1, 1)

        assertThat(trackpoints).hasSize(1)
        assertThat(trackpoints[0]).hasSize(1)
        assertThat(trackpoints[0][0]).isEqualTo(trackpoint)
    }

    @Test
    fun readTracksV2() {
        var trackpoint = createTrackpoint()
        every {
            contentResolver.query(
                testUri,
                TrackpointReader.PROJECTION_V2,
                "${TrackpointReader.ID}> ? AND ${TrackpointReader.TYPE} IN (-2, -1, 0, 1, 3)",
                arrayOf("1"),
                null
            )
        } returns cursor
        every { cursor.getColumnIndex(TrackpointReader.TYPE) } returns 7
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getLong(1) } returnsMany listOf(2)
        every { cursor.getLong(2) } returnsMany listOf(trackpoint.trackId!!)
        every { cursor.getInt(3) } returnsMany listOf(trackpoint.latLong!!.latitudeE6)
        every { cursor.getInt(4) } returnsMany listOf(trackpoint.latLong.longitudeE6)
        every { cursor.getLong(5) } returnsMany listOf(trackpoint.time!!.toEpochMilli())
        every { cursor.getDouble(6) } returnsMany listOf(trackpoint.speed!!)
        every { cursor.getInt(7) } returnsMany listOf(trackpoint.type)

        val trackpoints = TrackpointReader.readTrackpointsBySegments(contentResolver, testUri, 1, 2)

        assertThat(trackpoints).hasSize(1)
        assertThat(trackpoints[0]).hasSize(1)
        assertThat(trackpoints[0][0]).isEqualTo(trackpoint)
    }

    private fun createTrackpoint() = Trackpoint(
        id = 2L,
        trackId = 1L,
        latitude = 50.9,
        longitude = 9.1,
        type = TRACKPOINT_TYPE_TRACKPOINT,
        speed = 1.1,
        time = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    )
}