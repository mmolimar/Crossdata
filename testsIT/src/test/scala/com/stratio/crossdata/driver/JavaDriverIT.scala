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
package com.stratio.crossdata.driver

import java.nio.file.Paths

import com.stratio.crossdata.driver.config.DriverConf
import com.stratio.crossdata.driver.metadata.JavaTableName
import com.stratio.crossdata.driver.test.Utils._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JavaDriverIT extends EndToEndTest{

  driverFactories foreach { case (factory, description) =>

    implicit val ctx = DriverTestContext(factory)

    "JavaDriver (with default options)" should s"get a list of tables $description" in {

      assumeCrossdataUpAndRunning()
      withJavaDriverDo { javaDriver =>

        javaDriver.sql(
          s"CREATE TABLE db.jsonTable3 USING org.apache.spark.sql.json OPTIONS (path '${Paths.get(getClass.getResource("/tabletest.json").toURI()).toString}')"
        )
        javaDriver.sql(
          s"CREATE TABLE jsonTable3 USING org.apache.spark.sql.json OPTIONS (path '${Paths.get(getClass.getResource("/tabletest.json").toURI()).toString}')"
        )

        javaDriver.listTables() should contain allOf(new JavaTableName("jsonTable3", "db"), new JavaTableName("jsonTable3", ""))
      }
    }

    "JavaDriver (specifying serverHost, and flattened value)" should s"return a list of tables $description" in {

      assumeCrossdataUpAndRunning()

      withJavaDriverDo { javaDriver =>

        javaDriver.sql(
          s"CREATE TABLE db.jsonTable3 USING org.apache.spark.sql.json OPTIONS (path '${Paths.get(getClass.getResource("/tabletest.json").toURI()).toString}')"
        )
        javaDriver.sql(
          s"CREATE TABLE jsonTable3 USING org.apache.spark.sql.json OPTIONS (path '${Paths.get(getClass.getResource("/tabletest.json").toURI()).toString}')"
        )

        javaDriver.listTables() should contain allOf(new JavaTableName("jsonTable3", "db"), new JavaTableName("jsonTable3", ""))
      } (DriverTestContext(factory, Some(new DriverConf().setFlattenTables(true))))

    }

  }

}
