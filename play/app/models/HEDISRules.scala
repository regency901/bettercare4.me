/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import scala.util.Random
import org.joda.time.Interval
import org.joda.time.DateTime

/**
 * Trait to define an HEDIS rule.
 *
 * Implementation of this trait are able to generate claims for a patient meeting the rule criteria.
 * The generated claims will be compliant to the rule randomly based on targetCompliance rate (percentage)
 */
trait HEDISRule {

  /**
   * Indicate the name of the rule for configuration and reporting purpose
   */
  def name: String

  /**
   * Indicate the full name of the rule (human readable)
   */
  def fullName: String

  /**
   * Indicate the rule description (human readable)
   */
  def description: String

  /**
   * Indicates the rate at which the patients are eligible to the  measure.
   *
   * To be eligible, the patient must first meet the demographic requirements.
   * Example, an \c eligibleRate of 25 for CDC H1C, means that 25% of patient of age between 18 - 75
   * will have diabetes. Note that all CDC measure should have the same \c eligibleRate
   */
  def eligibleRate: Int

  /**
   * Indicates the rate at which the patients meet the measure, in %
   *
   * (patient in numerator, i.e., meet measure) / (patient in denominator, i.e., not excluded from measure) * 100
   *
   * This rate does not apply to exclusions (patients excluded from measure).
   *
   */
  def meetMeasureRate: Int

  /**
   * Indicates the rate at which patients are excluded from measure, in %
   *
   * Fraction of eligible patients that meet the exclusion criteria:
   * (excluded patients) / (eligible patients)
   */
  def exclusionRate: Int

  /**
   * Generate the claims for the patient to be in the denominator and possibly in the numerator as well.
   *
   * The patient is randomly in the numerator based on the \c targetCompliance rate.
   */
  def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim]

  /**
   * Verify if the measure is applicable to the patient based on patient's
   * demographics only.
   *
   * The patient may still not be eligible to the measure if the clinical criteria are not met.
   */
  def isPatientMeetDemographic(patient: Patient): Boolean

  /**
   * Verify if patient is eligible to the measure
   *
   * Patient may be eligible to the measure but excluded if meet the exclusion criteria.
   */
  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if patient meet the exclusion condition of the measure
   *
   * Does not verify if patient is eligible, but simply the exclusion criteria
   */
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure.
   */
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if the patient is in the denominator of the rule, i.e., eligible to the measure and not excluded.
   */
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean

}

abstract class HEDISRuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRule {

  def eligibleRate: Int = config.eligibleRate
  def meetMeasureRate: Int = config.meetMeasureRate
  def exclusionRate: Int = config.exclusionRate

  def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty

  def generateClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // Check if the patient is to be considered for the rule
    if (isPatientMeetDemographic(patient)) {

      // Check if patient is eligible
      if (Random.nextInt(100) < eligibleRate) {

        // Generate the claims to make the patient eligible
        val claims = generateEligibleClaims(pl, patient, provider)

        // Check if the patient will meet the exclusion criteria
        if (Random.nextInt(100) < exclusionRate) {

          // Generate the claim to meet the exclusion criteria
          List.concat(claims, generateExclusionClaims(pl, patient, provider))

        } else {

          // Check if the patient is in the measure
          if (Random.nextInt(100) < meetMeasureRate) {

            // Generate the claim to meet the measure
            List.concat(claims, generateMeetMeasureClaims(pl, patient, provider))

          } else {

            // Patient does not meet the measure
            claims
          }
        }

      } else {

        // Patient does not meet eligibility criteria
        List.empty
      }
      
    } else {

      // Patient does not meet demographics
      List.empty
    }
  }

  def isPatientMeetDemographic(patient: Patient): Boolean = true
  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientMeetDemographic(patient)
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean = false
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientEligible(patient, patientHistory) && !isPatientExcluded(patient, patientHistory)

  /**
   * Utility method to pick randomly one item from the list
   */
  def getOne[A](items: List[A]): A = items(Random.nextInt(items.size))
  
  /**
   * Utility method to get an \c Interval from the \c hedisDate to the nbr of specified days prior to it.
   * 
   * This interval exclude the hedisDate
   */
  def getInterval(nbrDays: Int): Interval = new Interval(hedisDate.minusDays(nbrDays), hedisDate)
}

object HEDISRules {

  val createRuleByName: Map[String, (RuleConfig, DateTime) => HEDISRule] = Map(
    "TEST" -> { (c, d) => new TestRule(c, d) },
    "BCS" -> { (c, d) => new BCSRule(c, d) })

}

// define all rules

/**
 * Breast Cancer Screening Rule
 */
class TestRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  def name = "TEST"
  def fullName = "Test Rule"
  def description = "This rule is for testing."

  override def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {
    val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
    List(
      persistenceLayer.createClaim(patient.patientID, provider.providerID, dos, dos, 
          icdDPri="icd 1", icdD=Set("icd 1", "icd 2"), icdP=Set("icd p1"), 
          hcfaPOS="hcfaPOS", ubRevenue="ubRevenue"))
  }
}

/**
 * Breast Cancer Screening Rule
 */
class BCSRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  def name = "BCS"
  def fullName = "Breast Cancer Screening"
  def description = "The percentage of women between 50 - 74 years of age who had a mammogram to screen for breast cancer any time on or between October 1 two years prior to the measurement year and December 31 of the measurement year (27 months)."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age > 49 && age < 75
  }

  // This rule has 100% eligibility when the demographics are meet
  override def eligibleRate: Int = 100

// Claim arguments:
//    patientID: String,
//    providerID: String,
//    dos: DateTime,
//    dosThru: DateTime,
//    claimStatus: String,
//    pcpFlag: String,
//    icdDPri: String,
//    icdD: Set[String],
//    icdP: Set[String],
//    hcfaPOS: String,
//    drg: String,
//    tob: String,
//    ubRevenue: String,
//    cpt: String,
//    cptMod1: String,
//    cptMod2: String,
//    hcpcs: String,
//    hcpcsMod: String,
//    dischargeStatus: String,
//    daysDenied: Int,
//    roomBoardFlag: String    

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {
    getOne(List(
      // One possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(30 * 365)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("85.42"), cpt="19180")) },
      // Another possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(30 * 365)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("85.41", "85.43"), cpt="19220")) },
      // Another possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(30 * 365)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("85.45", "85.47"), cpt="19240")) }))()
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {
    getOne(List(
      // One possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(27 * 30)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("87.36", "87.37"), ubRevenue="0401", cpt="77055", hcpcs="G0202")) },
      // Another possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(27 * 30)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("87.37"), ubRevenue="0403", cpt="77056", hcpcs="G0204")) },
      // Another possible set of claims
      { () => val dos=hedisDate.minusDays(Random.nextInt(27 * 30)); List(pl.createClaim(patient.patientID, provider.providerID, dos, dos, icdP=Set("87.36"), ubRevenue="0401", cpt="77057", hcpcs="G0206")) }))()
  }
//  
//  override def isPatientExcluded(patient: Patient, ph: PatientHistory): Boolean = {
//    
//    // Check if patient had Bilateral Mastectomy
//    
//    
//    // Check if patient had 2 Unilateral Mastectomy on 2 different dates
//    
//    // Check if patient had a Unilateral Mastectomy with bilateral modifier
//    
//    // Check if patient had a Unilateral Mastectomy code with a right (RT) side modifier 
//    // and a Unilateral Mastectomy with a left (LT) side modifier (may be on same or different dates of service)
//    
//  }

}
