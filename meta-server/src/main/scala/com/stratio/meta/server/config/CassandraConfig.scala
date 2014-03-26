/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.server.config

import com.typesafe.config.Config
import scala.collection.JavaConversions._

object CassandraConfig{

  val CASSANDRA_HOSTS_KEY = "server.cassandra.hosts"

  val CASSANDRA_PORT_KEY =  "server.cassandra.port"

}

trait CassandraConfig {
  def config: Config = ???

  lazy val cassandraHosts: Array[String] = config.getStringList(CassandraConfig.CASSANDRA_HOSTS_KEY).toList.toArray
  lazy val cassandraPort: Int = config.getInt(CassandraConfig.CASSANDRA_PORT_KEY)
}
