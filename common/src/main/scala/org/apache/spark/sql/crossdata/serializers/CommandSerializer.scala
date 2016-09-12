/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.crossdata.serializers

import java.util.UUID

import com.stratio.crossdata.common.{Command, SQLCommand}
import org.json4s._
import org.json4s.ext.UUIDSerializer

object CommandSerializer extends CustomSerializer[Command](
  format => (
    {
      case jsqlCommand @ JObject(JField("sql", _)::JField("queryId", _)::JField("flattenResults", _)::_) =>
        implicit val _ = DefaultFormats + UUIDSerializer
        jsqlCommand.extract[SQLCommand] //TODO: Test
    },
    {
      case command: SQLCommand => Extraction.decompose(command)(DefaultFormats + UUIDSerializer)
    }
    )
)
