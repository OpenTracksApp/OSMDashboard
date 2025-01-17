package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_PAUSE
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.TrackpointsDebug
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val PAUSE_LAT_LONG = (100 * APIConstants.LAT_LON_FACTOR).toInt()

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
        var trackpoint1 = createTrackpoint()
        var trackpoint2 =
            createTrackpoint().copy(id = 3L, type = TRACKPOINT_TYPE_PAUSE, speed = 0.0)

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
        every { cursor.moveToNext() } returnsMany listOf(true, true, true, false)
        every { cursor.getLong(1) } returnsMany listOf(trackpoint1.id!!, trackpoint2.id!!, 0)
        every { cursor.getLong(2) } returnsMany listOf(
            trackpoint1.trackId!!,
            trackpoint2.trackId!!,
            0
        )
        every { cursor.getInt(3) } returnsMany listOf(
            trackpoint1.latLong.latitudeE6,
            PAUSE_LAT_LONG,
            0
        )
        every { cursor.getInt(4) } returnsMany listOf(
            trackpoint1.latLong.longitudeE6,
            PAUSE_LAT_LONG,
            0
        )
        every { cursor.getLong(5) } returnsMany listOf(
            trackpoint1.time!!.toEpochMilli(),
            trackpoint2.time!!.toEpochMilli(),
            0
        )
        every { cursor.getDouble(6) } returnsMany listOf(
            trackpoint1.speed!!,
            trackpoint2.speed!!,
            0.0
        )

        val trackpoints = TrackpointReader.readTrackpointsBySegments(contentResolver, testUri, 1, 1)

        assertThat(trackpoints).hasSize(1)
        assertThat(trackpoints[0]).hasSize(2)
        assertThat(trackpoints[0][0]).isEqualTo(trackpoint1)
        assertThat(trackpoints[0][1]).isEqualTo(trackpoint2)
        assertThat(trackpoints.debug).isEqualTo(
            TrackpointsDebug(
                trackpointsReceived = 3,
                trackpointsInvalid = 1,
                trackpointsPause = 1,
                segments = 1,
            )
        )
    }

    @Test
    fun readTracksV2() {
        var trackpoint1 = createTrackpoint()
        var trackpoint2 =
            createTrackpoint().copy(id = 3L, type = TRACKPOINT_TYPE_PAUSE, speed = 0.0)
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
        every { cursor.moveToNext() } returnsMany listOf(true, true, true, false)
        every { cursor.getLong(1) } returnsMany listOf(trackpoint1.id!!, trackpoint2.id!!, 0)
        every { cursor.getLong(2) } returnsMany listOf(
            trackpoint1.trackId!!,
            trackpoint2.trackId!!,
            0
        )
        every { cursor.getInt(3) } returnsMany listOf(
            trackpoint1.latLong.latitudeE6,
            PAUSE_LAT_LONG,
            (130 * APIConstants.LAT_LON_FACTOR).toInt(),
        )
        every { cursor.getInt(4) } returnsMany listOf(
            trackpoint1.latLong.longitudeE6,
            PAUSE_LAT_LONG,
            0,
        )
        every { cursor.getLong(5) } returnsMany listOf(
            trackpoint1.time!!.toEpochMilli(),
            trackpoint2.time!!.toEpochMilli(),
            0,
        )
        every { cursor.getDouble(6) } returnsMany listOf(
            trackpoint1.speed!!,
            trackpoint2.speed!!,
            0.0,
        )
        every { cursor.getInt(7) } returnsMany listOf(
            trackpoint1.type,
            trackpoint2.type,
            TRACKPOINT_TYPE_TRACKPOINT,
        )

        val trackpoints = TrackpointReader.readTrackpointsBySegments(contentResolver, testUri, 1, 2)

        assertThat(trackpoints).hasSize(1)
        assertThat(trackpoints[0]).hasSize(2)
        assertThat(trackpoints[0][0]).isEqualTo(trackpoint1)
        assertThat(trackpoints[0][1]).isEqualTo(trackpoint2)
        assertThat(trackpoints.debug).isEqualTo(
            TrackpointsDebug(
                trackpointsReceived = 3,
                trackpointsInvalid = 1,
                trackpointsPause = 1,
                segments = 1,
            )
        )
    }

    private fun createTrackpoint() = Trackpoint(
        id = 2L,
        trackId = 1L,
        latLong = GeoPoint(50.9, 9.1),
        type = TRACKPOINT_TYPE_TRACKPOINT,
        speed = 1.1,
        time = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    )
}