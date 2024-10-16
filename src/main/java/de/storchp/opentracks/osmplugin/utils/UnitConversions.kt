/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.storchp.opentracks.osmplugin.utils

/**
 * Unit conversion constants.
 *
 * @author Sandor Dornbush
 */
object UnitConversions {
    // multiplication factor to convert seconds to milliseconds
    const val S_TO_MS: Long = 1000

    // Time
    // multiplication factor to convert milliseconds to seconds
    val MS_TO_S: Double = 1.0 / S_TO_MS

    // multiplication factor to convert minutes to seconds
    const val MIN_TO_S: Double = 60.0

    // multiplication factor to convert seconds to minutes
    val S_TO_MIN: Double = 1 / MIN_TO_S

    // multiplication factor to convert hours to minutes
    const val HR_TO_MIN: Double = 60.0

    // multiplication factor to convert minutes to hours
    val MIN_TO_HR: Double = 1 / HR_TO_MIN

    // multiplication factor to convert kilometers to miles
    const val KM_TO_MI: Double = 0.621371192

    // Distance
    // multiplication factor to convert miles to feet
    const val MI_TO_FT: Double = 5280.0

    // multiplication factor to covert kilometers to meters
    const val KM_TO_M: Double = 1000.0

    // multiplication factor to convert meters to kilometers
    val M_TO_KM: Double = 1 / KM_TO_M

    // multiplication factor to convert meters to miles
    val M_TO_MI: Double = M_TO_KM * KM_TO_MI

    // multiplication factor to convert meters to feet
    val M_TO_FT: Double = M_TO_MI * MI_TO_FT

    // multiplication factor to convert meters per second to kilometers per hour
    val MS_TO_KMH: Double = M_TO_KM / (S_TO_MIN * MIN_TO_HR)

    // Others
    // multiplication factor to convert degrees to radians
    val DEG_TO_RAD: Double = Math.PI / 180.0
}
