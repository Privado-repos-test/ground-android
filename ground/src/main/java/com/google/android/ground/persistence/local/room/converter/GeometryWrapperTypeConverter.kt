/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.persistence.local.room.converter

import androidx.room.TypeConverter
import com.google.android.ground.persistence.local.room.entity.GeometryWrapper
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.json.JSONException
import timber.log.Timber

object GeometryWrapperTypeConverter {

  @TypeConverter
  fun toByteArray(geometryWrapper: GeometryWrapper?): ByteArray? =
    geometryWrapper?.getGeometry().let { Cbor.encodeToByteArray(it) }

  @TypeConverter
  fun fromByteArray(jsonString: ByteArray?): GeometryWrapper? =
    try {
      jsonString?.let { GeometryWrapper.fromGeometry(Cbor.decodeFromByteArray(it)) }
    } catch (e: JSONException) {
      Timber.d(e, "Invalid Geometry in db")
      null
    }
}
