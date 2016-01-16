package com.stratio.crossdata.server

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}

import scala.util.Try

trait CassandraIT extends CassandraDefaultTestConstants with WordSpecLike with BeforeAndAfterAll {

  lazy val session = createSession()

  def createSession(): Session = {
    val session = Cluster.builder.addContactPoint(CassandraHost).build.connect
    assert(!session.isClosed, "Cannot create a Cassandra session")
    session
  }

  def createKeyspace(session: Session) = {
    val query =
      s"""CREATE KEYSPACE $Catalog WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}
         |  AND durable_writes = true;""".stripMargin
    session.execute(query)
  }

  def createTable(session: Session) = {
    val query = s"CREATE TABLE $Catalog.$Table (date timestamp PRIMARY KEY)"
    session.execute(query)
  }

  def insertData(session: Session) = {
    val query = s"INSERT INTO $Catalog.$Table(date) VALUES ('2015-06-23 10:30+0100')"
    session.execute(query)
  }

  override protected def beforeAll() = {
    createKeyspace(session)
    createTable(session)
    insertData(session)
  }

  def dropKeyspace(session: Session) = {
    val query = s"DROP KEYSPACE $Catalog"
    session.execute(query)
  }

  def closeSession(session: Session) = {
    session.close
  }

  override protected def afterAll(): Unit = {
    dropKeyspace(session)
    closeSession(session)
  }
}

sealed trait CassandraDefaultTestConstants {

  def uuid = java.util.UUID.randomUUID.toString

  val ClusterName = "Test Cluster"
  val Catalog = s"tests-${uuid}"
  val Table = "myTable"
  val CassandraHost: String = {
    Try(ConfigFactory.load().getStringList("cassandra.hosts")).map(_.get(0)).getOrElse("127.0.0.1")
  }
  val SourceProvider = "com.stratio.crossdata.connector.cassandra"
}
