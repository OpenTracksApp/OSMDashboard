# <img src="drawable-svg/LOGO.svg" alt="OpenTracks logo" height="40"></img> OpenTracks - OSM Dashboard

_OSM Dashboard_ is a companion application for [OpenTracks](https://github.com/OpenTracksApp/OpenTracks).
It adds the functionality to show tracks on a map using data from [OpenStreetMap](https://www.openstreetmap.org).
OSM Dashboard updates the provided data from OpenTracks in real-time.

<table>
    <tr>
        <th>Free</th>
        <th>Free</th>
    </tr>
    <tr>
        <td>
            <a href="https://f-droid.org/en/packages/de.storchp.opentracks.osmplugin/">
                <img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="60" align="middle">
            </a>
        </td>
        <td>
            <a href="https://play.google.com/store/apps/details?id=de.storchp.opentracks.osmplugin">
                <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" align="middle">
            </a>
        </td>        
    </tr>
</table>

## Features:
* _Online map data_: download map data on-demand
* _Offline map data_: use on-device stored map data
* _Map themes_: support of custom map themes
    
__Required permissions:__
* _INTERNET_: required for online maps
* _READ_EXTERNAL_STORAGE_: required for offline maps

## Maps

The map implementation is based on the [Mapsforge](https://github.com/mapsforge/mapsforge) library. This also defines the types of offline maps which can be used.

The default online map is provided by [OpenStreetMap.org](https://openstreetmap.org).
Join the community and help to improve the map, see [www.openstreetmap.org/fixthemap](https://www.openstreetmap.org/fixthemap).

Please consider downloading an offline map to decrease the server load and save your mobile data plan.

Some offline maps can be found here:

- [Mapsforge](http://download.mapsforge.org/)
- [Freizeitkarte Android](https://www.freizeitkarte-osm.de/android/en/)
- [OpenAndroMaps](https://www.openandromaps.org/en)

Some maps require special themes to render correctly! These need to be downloaded and configured accordingly.

To use offline maps, put them on your Android device external storage (e.g. `/storage/emulated/0`, depending on your device) and select the directory in the App menu `Maps` / `Offline Map Directory`.
