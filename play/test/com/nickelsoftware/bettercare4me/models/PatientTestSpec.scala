/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models;

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime

class PatientTestSpec extends PlaySpec {

  "The Patient class" must {

    "be created with valid arguments" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.patientID mustBe "key1"
      patient.firstName mustBe "Michel"
      patient.lastName mustBe "Dufresne"
      patient.gender mustBe "M"
      patient.dob mustBe new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay()
    }

    "compute age in years correctly" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.age(new LocalDate(2014, 7, 27).toDateTimeAtStartOfDay()) mustBe 52
      patient.age(new LocalDate(2014, 9, 30).toDateTimeAtStartOfDay()) mustBe 52
      patient.age(new LocalDate(2014, 6, 1).toDateTimeAtStartOfDay()) mustBe 51
    }

    "compute age in months correctly" in {
      val patient = Patient("key1", "Sophie", "Dufresne", "F", LocalDate.parse("2000-07-27").toDateTimeAtStartOfDay())

      patient.ageInMonths(new LocalDate(2000, 9, 27).toDateTimeAtStartOfDay()) mustBe 2
      patient.ageInMonths(new LocalDate(2001, 7, 26).toDateTimeAtStartOfDay()) mustBe 11
      patient.ageInMonths(new LocalDate(2001, 7, 27).toDateTimeAtStartOfDay()) mustBe 12
      patient.ageInMonths(new LocalDate(2001, 8, 28).toDateTimeAtStartOfDay()) mustBe 13
    }

    "put all atributes into a List" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", LocalDate.parse("1962-07-27").toDateTimeAtStartOfDay())

      patient.toList mustBe List("key1", "Michel", "Dufresne", "M", "1962-07-27T00:00:00.000-04:00")
    }

    "create a Patient from a list of attributes" in {
      val patient = Patient("key1", "Michel", "Dufresne", "M", DateTime.parse("1962-07-27T00:00:00.000-04:00"))

      PatientParser.fromList(patient.toList) mustBe patient

      // just to make sure ...
      val l = List("key1", "Michel", "Dufresne", "M", "1962-07-27T00:00:00.000-04:00")
      PatientParser.fromList(l) mustBe patient
      PatientParser.fromList(l) mustEqual patient
      
    }
  }

  "The PatientHistoryFactory class" must {

    "create PatientHistory from empty list of claims" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)
      PatientHistoryFactory.createPatientHistory(patient, List()) mustBe PatientHistory(MedMap(), RxMap(), LabMap())
    }

    "create PatientHistory from a single claim with no codes" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)
      PatientHistoryFactory.createPatientHistory(patient, List(MedClaimTestSpecHelper.mkClaim0)) mustBe PatientHistory(MedMap(), RxMap(), LabMap())
    }

    "create PatientHistory from a single medical claim with codes" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)

      val cl = List(MedClaimTestSpecHelper.mkClaim)
      val medMap = MedMap(
          Map(("specialtyCde" -> cl)), Map(("hcfaPOS" -> cl)), 
          Map(("icdDPri" -> cl), ("icd 1" -> cl), ("icd 2" -> cl)), Map(("icd p1" -> cl)), Map(("cpt" -> cl)), 
          Map(("tob" -> cl)), Map(("ubRevenue" -> cl)), Map(("hcpcs" -> cl)))

      PatientHistoryFactory.createPatientHistory(patient, cl) mustBe
        PatientHistory(medMap, RxMap(), LabMap())
    }

    "create PatientHistory from multiple claims" in {
      val date = LocalDate.parse("2014-09-05").toDateTimeAtStartOfDay()
      val patient = Patient("key1", "Michel", "Dufresne", "M", date)

      val c1 = MedClaim("c1", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, date, MHead(specialtyCde="spe1", hcfaPOS="pos1"), MCodes(icdDPri="icd 1", icdD=Set("icd 2", "icd 3"), icdP=Set("icd p1"), cpt="cpt1"), MBill(tob="tob1", ubRevenue="ubRevenue", hcpcs="hcpcs1"))
      val c2 = MedClaim("c2", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, date, MHead(specialtyCde="spe1"),                 MCodes(icdDPri="icd 1", icdD=Set("icd 2", "icd 3"), icdP=Set("icd p1"), cpt="cpt2"), MBill(            ubRevenue="ubRevenue", hcpcs="hcpcs"))
      val c3 = MedClaim("c3", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, date, MHead(                     hcfaPOS="pos1"), MCodes(icdDPri="icd 4", icdD=Set(         "icd 3"), icdP=Set("icd p1"), cpt="cpt3"), MBill(tob="tob1", ubRevenue="ubRevenue", hcpcs="hcpcs"))
      val c4 = RxClaim("c4", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, ndc = "ndc 1")
      val c5 = LabClaim("c5", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, cpt = "cpt L1", loinc = "loinc 1")
      val c6 = LabClaim("c6", "p1", "p1.first", "p1.last", "p2", "p2.first", "p2.last", date, cpt = "cpt L1", loinc = "loinc 2")

      val cl1 = List(c1)
      val cl2 = List(c2)
      val cl3 = List(c3)
      val cl12 = List(c2, c1)
      val cl23 = List(c3, c2)
      val cl13 = List(c3, c1)
      val cl123 = List(c3, c2, c1)

      val ph = PatientHistoryFactory.createPatientHistory(patient, List(c1, c2, c3, c4, c5, c6))
      
      ph.specialtyCde  mustBe Map(("spe1" -> cl12))
      ph.claims4Specialty("spe1") mustBe cl12
      ph.claims4Specialty("xxxx") mustBe List[MedClaim]()
      
      ph.hcfaPOS  mustBe Map(("pos1" -> cl13))
      ph.claims4HCFAPOS("pos1") mustBe cl13
      ph.claims4HCFAPOS("xxxx") mustBe List[MedClaim]()
      
      ph.icdD      mustBe Map(("icd 1" -> cl12), ("icd 2" -> cl12), ("icd 3" -> cl123), ("icd 4" -> cl3))
      ph.claims4ICDD("icd 1") mustBe cl12
      ph.claims4ICDD("xxxx") mustBe List[MedClaim]()
      
      ph.icdP      mustBe Map(("icd p1" -> cl123))
      ph.claims4ICDP("icd p1") mustBe cl123
      ph.claims4ICDP("xxxx") mustBe List[MedClaim]()
      
      ph.cpt       mustBe Map(("cpt1" -> cl1), ("cpt2" -> cl2), ("cpt3" -> cl3))
      ph.claims4CPT("cpt1") mustBe cl1
      ph.claims4CPT("xxxx") mustBe List[MedClaim]()
      
      ph.tob  mustBe Map(("tob1" -> cl13))
      ph.claims4TOB("tob1") mustBe cl13
      ph.claims4TOB("xxxx") mustBe List[MedClaim]()
      
      ph.ubRevenue mustBe Map(("ubRevenue" -> cl123))
      ph.claims4UBRev("ubRevenue") mustBe cl123
      ph.claims4UBRev("xxxx") mustBe List[MedClaim]()
      
      ph.hcpcs     mustBe Map(("hcpcs1" -> cl1), ("hcpcs" -> cl23))
      ph.claims4HCPCS("hcpcs") mustBe cl23
      ph.claims4HCPCS("xxxx") mustBe List[MedClaim]()

      ph.ndc		mustBe Map(("ndc 1" -> List(c4)))
      ph.claims4NDC("ndc 1") mustBe List(c4)
      ph.claims4NDC("xxxx") mustBe List[RxClaim]()

      ph.cptLab		mustBe Map(("cpt L1" -> List(c6, c5)))
      ph.claims4CPTLab("cpt L1") mustBe List(c6, c5)
      ph.claims4CPTLab("xxxx") mustBe List[LabClaim]()

      ph.loinc		mustBe Map(("loinc 1" -> List(c5)), ("loinc 2" -> List(c6)))
      ph.claims4LOINC("loinc 1") mustBe List(c5)
      ph.claims4LOINC("xxxx") mustBe List[LabClaim]()
    }
  }

  "The SimplePersistenceLayer class" must {

    "create Patient with sequential keys" in {
      val persistenceLayer = new SimplePersistenceLayer(99)

      val dob = new LocalDate(1962, 7, 27).toDateTimeAtStartOfDay()
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-0", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-1", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-2", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-3", "Michel", "Dufresne", "M", dob)
      persistenceLayer.createPatient("Michel", "Dufresne", "M", dob) mustBe Patient("patient-99-4", "Michel", "Dufresne", "M", dob)
    }
  }
}
