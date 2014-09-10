/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package testActors;

import play.api.Play.current
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import play.api.test.WithApplication
import actors.ClaimGeneratorActor
import org.joda.time.LocalDate
import java.io.File
import com.github.tototoshi.csv.CSVReader

class EventCrawlerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  import ClaimGeneratorActor._

  val claimGeneratorActor = system.actorOf(Props[ClaimGeneratorActor])

  def this() = this(ActorSystem("EventCrawlerSpec"))
  val basePath = "./data/temp-test-"
  val pathName = basePath + LocalDate.now().toString()

  override def afterAll {

    // post test
    val dir = new File(pathName)
    for (f <- dir.listFiles()) f.delete()
    dir.delete()

    TestKit.shutdownActorSystem(system)
  }

  "A ClaimGeneratorActor actor" must {

    "generate Patients, Providers and Claims based on configuration triggered by a message" in new WithApplication {

      val baseFname = "ClaimGenerator"
      val nbrGen = 2
      claimGeneratorActor ! GenerateClaimsRequest("""
        basePath: """ + basePath + """
        baseFname: """ + baseFname + """
        nbrGen: """ + nbrGen + """
        nbrPatients: 1
        nbrProviders: 1
        maleNamesFile: ./data/male-names.csv
        femaleNamesFile: ./data/female-names.csv
        lastNamesFile: ./data/last-names.csv
        hedisDateTxt: 2014-01-01
        rulesConfig:
          - name: TEST
            eligibleRate: 100
            meetMeasureRate: 100
            exclusionRate: 0
          """)

      expectMsg(GenerateClaimsCompleted(0))

      (new File(basePath + LocalDate.now().toString())).listFiles() should have length 6

      val fnameBase = pathName + "/" + baseFname

      for (igen <- 1 to nbrGen) {

        CSVReader.open(new File(fnameBase + "_patients_" + igen + ".csv")).all() should have length 1
        CSVReader.open(new File(fnameBase + "_providers_" + igen + ".csv")).all() should have length 1
        CSVReader.open(new File(fnameBase + "_claims_" + igen + ".csv")).all() should have length 1
      }
    }
  }
}