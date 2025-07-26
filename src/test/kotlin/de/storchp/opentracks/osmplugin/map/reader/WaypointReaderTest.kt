package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint

class WaypointReaderTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var cursor: Cursor
    private val testUri: Uri = mockk()

    @BeforeEach
    fun setUp() {
        contentResolver = mockk()
        cursor = mockk()

        every { cursor.getColumnIndexOrThrow(WaypointReader.ID) } returns 1
        every { cursor.getColumnIndexOrThrow(WaypointReader.NAME) } returns 2
        every { cursor.getColumnIndexOrThrow(WaypointReader.DESCRIPTION) } returns 3
        every { cursor.getColumnIndexOrThrow(WaypointReader.CATEGORY) } returns 4
        every { cursor.getColumnIndexOrThrow(WaypointReader.ICON) } returns 5
        every { cursor.getColumnIndexOrThrow(WaypointReader.TRACKID) } returns 6
        every { cursor.getColumnIndexOrThrow(WaypointReader.LATITUDE) } returns 7
        every { cursor.getColumnIndexOrThrow(WaypointReader.LONGITUDE) } returns 8
        every { cursor.getColumnIndexOrThrow(WaypointReader.PHOTOURL) } returns 9
        every { cursor.close() } just Runs
    }

    @Test
    fun testReadWaypoints() {
        val waypoint2 = createWaypoint().copy(id = 2, latLong = GeoPoint(0, 0))
        val waypoint3 = createWaypoint().copy(id = 3)
        every {
            contentResolver.query(
                testUri,
                WaypointReader.PROJECTION,
                null,
                null,
                null
            )
        } returns cursor
        every { cursor.moveToNext() } returnsMany listOf(true, true, true, false)
        every { cursor.getLong(1) } returnsMany listOf(1, 2, 3)
        every { cursor.getString(2) } returnsMany listOf(waypoint2.name, waypoint3.name)
        every { cursor.getString(3) } returnsMany listOf(
            waypoint2.description,
            waypoint3.description
        )
        every { cursor.getString(4) } returnsMany listOf(waypoint2.category, waypoint3.category)
        every { cursor.getString(5) } returnsMany listOf(waypoint2.icon, waypoint3.icon)
        every { cursor.getLong(6) } returnsMany listOf(waypoint2.trackId!!, waypoint3.trackId!!)
        every { cursor.getInt(7) } returnsMany listOf(
            waypoint2.latLong.latitudeE6,
            waypoint3.latLong.latitudeE6
        )
        every { cursor.getInt(8) } returnsMany listOf(
            waypoint2.latLong.longitudeE6,
            waypoint3.latLong.longitudeE6
        )
        every { cursor.getString(9) } returnsMany listOf(waypoint2.photoUrl, waypoint3.photoUrl)

        val waypoints = WaypointReader.readWaypoints(contentResolver, testUri, 1)

        assertThat(waypoints).hasSize(1)
        assertThat(waypoints[0]).isEqualTo(waypoint3)
    }

    private fun createWaypoint() = Waypoint(
        id = 1L,
        name = "Test Waypoint",
        description = "Test Description",
        category = "Test Category",
        icon = "Test Icon",
        trackId = 1L,
        latLong = GeoPoint(50.9, 9.1),
        photoUrl = "http://example.com/photo.jpg",
    )
}