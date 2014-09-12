/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import utils.NickelException
import org.joda.time.DateTime

object ClaimParser {

  def toSet(l: List[String]): Set[String] = { (for (s <- l if s.length > 0) yield s).toSet }

  def fromList(l: List[String]): Claim = {
    if (l.size < 40) throw NickelException("ClaimParser.fromList - list must have a least 40 elements, have " + l.size)

    Claim(
      l(0), //1 claimID claim ID
      l(1), //2 patientID Patient ID 
      l(2), //3 providerID Provider ID
      LocalDate.parse(l(3)).toDateTimeAtStartOfDay(), //4 dos Date of Service
      LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), //5 dosThru DOS Thru
      l(5), //6 claimStatus Claim status
      l(6), //7 pcpFlag PCP Flag
      l(7), //8 icdDPri ICD Primary Diagnostic
      toSet(l.slice(8, 18)), //9-18 icdD Secondary Diagnostic codes (up to 10)
      toSet(l.slice(18, 28)), //19-28 icdP ICD Procedure codes (up to 10)
      l(28), //29 hcfaPOS HCFA Form 1500 POS (Point of Service),
      l(29), //30 drg Diagnosis Related Group
      l(30), //31 tob Type of Bill (3 chars)
      l(31), //32 ubRevenue UB Revenue (billing code) 
      l(32), //33 cpt CPT (procedure procedure)
      l(33), //34 cptMod1 CPT Modifier 1 (2 chars)
      l(34), //35 cptMod2 CPT Modifier 1 (2 chars)
      l(35), //36 hcpcs HCPCS (medical goods and services)
      l(36), //37 hcpcsMod HCPCS Modifier code (2 chars)
      l(37), //38 dischargeStatus Discharge Status (2 chars)
      l(38).toInt, //39 daysDenied Nbr of days denied for in-patient claims
      l(39) //40 roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
      )
  }
}

/**
 * Represent Institutional and Professional Claims
 *
 * The parameters to the class are:
 * 	- claimID Claim ID
 *  - patientID Patient ID
 *  - providerID Provider ID
 *  - dos Date of Service
 *  - dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
 *  - claimStatus Claim status:
 * 		"A" Adjustment to original claim
 * 		"D" Denied claims
 * 		"I" Initial Paid Claim
 * 		"P" Pended for adjudication
 * 		"R" Reversal to original claim
 *  - pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
 * 	- icdDPri ICD Primary Diagnostic
 *  - icdD Secondary Diagnostic codes (up to 10)
 *  - drg Diagnosis Related Group
 *  - tob Type of Bill (3 chars)
 *  - ubRevenue UB Revenue (billing code)
 *  - cpt CPT (procedure procedure)
 *  - cptMod1 CPT Modifier 1 (2 chars)
 *  - cptMod2 CPT Modifier 2 (2 chars)
 *  - hcpcs HCPCS (medical goods and services)
 *  - hcpcsMod HCPCS Modifier code (2 chars)
 *  - dischargeStatus Discharge Status (2 chars)
 *  - daysDenied Nbr of days denied for in-patient claims
 *  - roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
 */
case class Claim(

  //1 claimID Claim ID
  claimID: String,

  //2 patientID Patient ID
  patientID: String,

  //3 providerID Provider ID
  providerID: String,

  //4 dos Date of Service
  dos: DateTime,

  //5 dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
  dosThru: DateTime,

  //6 claimStatus Claim status: 
  //	"A" Adjustment to original claim
  //	"D" Denied claims
  // 	"I" Initial Paid Claim
  // 	"P" Pended for adjudication
  //	"R" Reversal to original claim
  claimStatus: String = "",

  //7 pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
  pcpFlag: String = "",

  // ICD-9 / ICD-10 CM (diagnostic codes)
  //8 icdDPri ICD Primary Diagnostic
  icdDPri: String = "",

  //9-18 icdD Secondary Diagnostic codes (up to 10)
  icdD: Set[String] = Set(),

  // ICD-9 / ICD-10 PCS (procedure codes)
  //19-28 icdP ICD Procedure codes (up to 10)
  icdP: Set[String] = Set(),

  //29 hcfaPOS HCFA Form 1500 POS (Point of Service),
  hcfaPOS: String = "",

  //30 drg Diagnosis Related Group
  drg: String = "",

  //31 tob Type of Bill (3 chars)
  tob: String = "",

  //32 ubRevenue UB Revenue (billing code) 
  ubRevenue: String = "",

  //33 cpt CPT (procedure procedure)
  cpt: String = "",

  //34 cptMod1 CPT Modifier 1 (2 chars)
  cptMod1: String = "",

  //35 cptMod2 CPT Modifier 1 (2 chars)
  cptMod2: String = "",

  //36 hcpcs HCPCS (medical goods and services)
  hcpcs: String = "",

  //37 hcpcsMod HCPCS Modifier code (2 chars)
  hcpcsMod: String = "",

  //38 dischargeStatus Discharge Status (2 chars)
  dischargeStatus: String = "",

  //39 daysDenied Nbr of days denied for in-patient claims
  daysDenied: Int = 0,

  //40 roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
  roomBoardFlag: String = "N") {

  def toList: List[String] = {

    List.concat(
      List(claimID, patientID, providerID, dos.toLocalDate().toString, dosThru.toLocalDate().toString, claimStatus, pcpFlag, icdDPri),
      icdD.toList,
      List.fill(10 - icdD.size)(""),
      icdP.toList,
      List.fill(10 - icdP.size)(""),
      List(hcfaPOS, drg, tob, ubRevenue, cpt, cptMod1, cptMod2, hcpcs, hcpcsMod, dischargeStatus, daysDenied.toString, roomBoardFlag))

  }
}



