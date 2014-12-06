/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import scala.collection.mutable.HashMap
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Random

import com.nickelsoftware.bettercare4me.cassandra.Bettercare4me
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.PersonGenerator
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer

/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
case object ClaimCassandraGeneratorHelper extends ClaimGeneratorHelper {

  /**
   * Generate claims using simulation parameters from `config
   *
   * Generate the simulated `Patients, `Providers, and `Claims to Cassandra.
   * This simulator uses `SimplePersistenceLayer for created the entities `UUIDs
   *
   * @param igen Generation number (batch ID)
   * @param config the generator's configuration parameters
   */
  def generateClaims(igen: Int, config: ClaimGeneratorConfig): ClaimGeneratorCounts = {

    // The persistence layer provides an abstraction level to the UUID generation
    val persistenceLayer = new SimplePersistenceLayer(igen)

    // Person generator class
    val personGenerator = new PersonGenerator(config.maleNamesFile, config.femaleNamesFile, config.lastNamesFile, config.hedisDate, persistenceLayer)

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

    // generate the providers
    val providers = (for (i <- 1 to config.nbrProviders) yield personGenerator.generateProvider).toList

    // insert them into cassandra
    Bettercare4me.batchProviders(igen, providers)

    // generate the patients
    val patients = (for (i <- 1 to config.nbrPatients) yield personGenerator.generatePatient).toList

    // insert them into cassandra
    Bettercare4me.batchPatients(igen, patients)

    // generate the claims
    var simScores = HashMap[String, (Int, Int, Int)]()
    var nbrClaims = 0L
    for {
      patient <- patients
      provider = getOne(providers)
    } {
      simScores.clear
      for {
        rule <- rules
        simScoreTpl = simScores.getOrElseUpdate(rule.config.simParityRuleName, (Random.nextInt(100), Random.nextInt(100), Random.nextInt(100)))
        claim <- rule.generateClaims(persistenceLayer, patient, provider, simScoreTpl._1, simScoreTpl._2, simScoreTpl._3)
      } {
        val l = List(claim)
        Bettercare4me.batchClaimsByPatients(igen, l)
        Bettercare4me.batchClaimsByProviders(igen, l)
        nbrClaims = nbrClaims + 1
      }
    }

    ClaimGeneratorCounts(patients.size.toLong, providers.size.toLong, nbrClaims)
  }

  def processGeneratedClaims(igen: Int, config: ClaimGeneratorConfig): HEDISScoreSummary = {

    val patientsFuture = Bettercare4me.queryPatients(igen)
    val claimsMapFuture = Bettercare4me.queryClaims(igen) map { c => c groupBy { _.patientID } }

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

    val patientsHistoryFuture = for {
      patients <- patientsFuture
      claimsMap <- claimsMapFuture
    } yield patients map { p => (p, PatientHistoryFactory.createPatientHistory(p, claimsMap.getOrElse(p.patientID, List.empty).toList)) }

    val scorecardsFuture = patientsHistoryFuture map (_ map {
      case (p, ph) =>
        val scorecard = rules.foldLeft(Scorecard())({ (scorecard, rule) => rule.scoreRule(scorecard, p, ph) })
        //** save patient scorecard to cassandra herre
        scorecard
    })

    // fold all the patients scorecards into a HEDISScoreSummary and return it
    val f = scorecardsFuture map { _.foldLeft(HEDISScoreSummary(rules))({ (scoreSummary, scorecard) => scoreSummary.addScoreCard(scorecard) }) }
    Await.result(f, Duration.Inf)
  }
}