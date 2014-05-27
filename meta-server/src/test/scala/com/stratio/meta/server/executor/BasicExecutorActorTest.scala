package com.stratio.meta.server.executor

import com.stratio.meta.core.engine.Engine
import akka.actor.ActorSystem
import com.stratio.meta.server.actors.ExecutorActor
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuiteLike
import scala.concurrent.duration._
import org.testng.Assert._
import com.stratio.meta.server.config.BeforeAndAfterCassandra
import com.stratio.meta.server.utilities._
import scala.collection.mutable


/**
 * To generate unit test of proxy actor
 */
class BasicExecutorActorTest extends TestKit(ActorSystem("TestKitUsageExectutorActorSpec",
  ConfigFactory.parseString(TestKitUsageSpec.config)))
with DefaultTimeout with FunSuiteLike with  BeforeAndAfterCassandra{

  lazy val engine:Engine =  createEngine.create()

  lazy val executorRef = system.actorOf(ExecutorActor.props(engine.getExecutor),"TestExecutorActor")


  override def beforeCassandraFinish() {
    shutdown(system)
  }

  override def afterAll() {
    super.afterAll()
    engine.shutdown()
  }

  val querying= new queryString

  test ("executor Test"){
    within(5000 millis){
      executorRef ! 1
      expectNoMsg()
    }
  }

  test ("QueryActor create KS"){
    within(5000 millis){
      val msg= "create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor create KS yet"){
    within(5000 millis){
      val msg="create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"Keyspace ks_demo already exists." )
    }
  }

  test ("QueryActor use KS"){
    within(5000 millis){
      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor describe keyspace"){
    within(5000 millis){
      val msg="describe keyspace system;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"success" )
    }
  }

  test ("QueryActor describe table"){
    within(5000 millis){
      val msg="describe table system.schema_columns;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"success" )
    }
  }

  test ("QueryActor use KS yet"){
    within(5000 millis){
      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }



  test ("QueryActor insert into table not create yet without error"){
    within(5000 millis){
      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"Table demo does not exist." )
    }
  }

  test ("QueryActor select without table"){
    within(5000 millis){
      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"Table demo does not exist.")
    }
  }

  test ("QueryActor create table not create yet"){
    within(5000 millis){
      val msg="create TABLE demo (field1 varchar PRIMARY KEY , field2 varchar);"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor create table  create yet"){
    within(5000 millis){
      val msg="create TABLE demo (field1 varchar PRIMARY KEY , field2 varchar);"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"Table demo already exists." )
    }
  }

  test ("QueryActor insert into table  create yet without error"){
    within(5000 millis){
      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor select"){
    within(5000 millis){
      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),mutable.MutableList("test1", "text2").toString() )
    }
  }

  test ("QueryActor drop table "){
    within(5000 millis){
      val msg="drop table demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor drop KS "){
    within(5000 millis){
      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"sucess" )
    }
  }

  test ("QueryActor drop KS  not exit"){
    within(5000 millis){
      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,executorRef,engine,1),"Keyspace ks_demo does not exist." )
    }
  }


}
