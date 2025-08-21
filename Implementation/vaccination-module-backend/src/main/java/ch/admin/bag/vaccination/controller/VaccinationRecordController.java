/**
 * Copyright (c) 2022 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.admin.bag.vaccination.controller;

import ch.admin.bag.vaccination.data.request.TranslationsRequest;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.admin.bag.vaccination.service.PdfService;
import ch.admin.bag.vaccination.service.VaccinationRecordService;
import ch.admin.bag.vaccination.service.VaccinationService;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * Vaccination-record controller of the module
 *
 */
@RestController
@RequestMapping("/vaccinationRecord")
@Tag(name = "VaccinationRecordController", description = "Vaccination record operation of the application")
@Slf4j
public class VaccinationRecordController {

  @Autowired
  private VaccinationService vaccinationService;

  @Autowired
  private VaccinationRecordService vaccinationRecordService;

  @Autowired
  private PdfService pdfService;

  @PostMapping("/communityIdentifier/{communityIdentifier}/convert")
  @Operation(summary = "Convert a Vaccination Record to a Immunization Administration document")
  public VaccinationRecordDTO convert(
      @Schema(example = "EPDPLAYGROUND") @PathVariable String communityIdentifier) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifierFromSession = HttpSessionUtils.getPatientIdentifierFromSession();
    PatientIdentifier patientIdentifierFromEPD = vaccinationService.getPatientIdentifier(communityIdentifier,
        patientIdentifierFromSession.getLocalAssigningAuthority(), patientIdentifierFromSession.getLocalExtenstion());
    return vaccinationRecordService.convertVaccinationToImmunization(patientIdentifierFromEPD, assertion);
  }

  @PostMapping("/communityIdentifier/{communityIdentifier}")
  @Operation(summary = "Create a vaccination record")
  public void create(
      HttpServletRequest request,
      @Schema(example = "EPDPLAYGROUND") @PathVariable String communityIdentifier,
      @RequestBody VaccinationRecordDTO record) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifierFromSession = HttpSessionUtils.getPatientIdentifierFromSession();
    vaccinationRecordService.create(communityIdentifier, patientIdentifierFromSession.getLocalAssigningAuthority(),
        patientIdentifierFromSession.getLocalExtenstion(), record, assertion);
  }

  @PostMapping("/exportToPDF/{lang}")
  @Operation(summary = "Build a PDF out of the records")
  public ResponseEntity<?> exportToPDF(@RequestBody TranslationsRequest translationsRequest, @PathVariable String lang) {
    log.info("Generating PDF...");

    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    VaccinationRecordDTO vaccinationRecord = vaccinationRecordService.create(patientIdentifier, assertion);
    vaccinationRecord.setI18nTargetDiseases(translationsRequest.getTargetDiseases());
    vaccinationRecord.setLang(lang);
    log.debug("exportToPDF {}", vaccinationRecord);

    InputStream inputStream = pdfService.create(vaccinationRecord, translationsRequest);
    InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(inputStreamResource);
  }

  @GetMapping("/communityIdentifier/{communityIdentifier}")
  @Operation(summary = "Get the data of the vaccination record")
  public VaccinationRecordDTO getAll(
      HttpServletRequest request,
      @Schema(example = "EPDPLAYGROUND") @PathVariable String communityIdentifier) throws InterruptedException {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifierFromSession = HttpSessionUtils.getPatientIdentifierFromSession();
    PatientIdentifier patientIdentifierFromEPD = vaccinationService.getPatientIdentifier(communityIdentifier,
        patientIdentifierFromSession.getLocalAssigningAuthority(), patientIdentifierFromSession.getLocalExtenstion());

    return vaccinationRecordService.create(patientIdentifierFromEPD, assertion);
  }

  @GetMapping("/communityIdentifier/{communityIdentifier}/name")
  @Operation(summary = "Get full name of a patient")
  public String getPatientName(
      @Schema(example = "EPDPLAYGROUND") @PathVariable String communityIdentifier) {
    PatientIdentifier patientIdentifierFromSession = HttpSessionUtils.getPatientIdentifierFromSession();
    PatientIdentifier patientIdentifierFromEPD = vaccinationService.getPatientIdentifier(communityIdentifier,
        patientIdentifierFromSession.getLocalAssigningAuthority(), patientIdentifierFromSession.getLocalExtenstion());

    return patientIdentifierFromEPD.getPatientInfo().getFullName();
  }
}
