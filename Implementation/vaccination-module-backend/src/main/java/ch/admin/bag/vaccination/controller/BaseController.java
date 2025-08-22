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

import ch.admin.bag.vaccination.service.BaseServiceIfc;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 *
 * Generic controller of the module
 *
 */
public abstract class BaseController<T extends BaseDTO> {
  protected BaseServiceIfc<T> service;

  protected BaseController(BaseServiceIfc<T> service) {
    this.service = service;
  }

  @PostMapping("/communityIdentifier/{communityIdentifier}")
  @Operation(description = "Create an entry")
  public ResponseEntity<T> create(
      HttpServletRequest request,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @RequestBody T newDTO) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    T result = service.create(communityIdentifier, patientIdentifier.getLocalAssigningAuthority(),
        patientIdentifier.getLocalExtenstion(), newDTO, assertion, false);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @DeleteMapping("/communityIdentifier/{communityIdentifier}/uuid/{uuid}")
  @Operation(description = "Delete an entry")
  public ResponseEntity<T> delete(
      HttpServletRequest request,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody ValueDTO confidentiality) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    T result = service.delete(communityIdentifier, patientIdentifier.getLocalAssigningAuthority(),
        patientIdentifier.getLocalExtenstion(), uuid, confidentiality, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @GetMapping("/communityIdentifier/{communityIdentifier}")
  @Operation(summary = "Get the data of the entries")
  public ResponseEntity<List<T>> getAll(
      HttpServletRequest request,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    return new ResponseEntity<>(service.getAll(communityIdentifier, patientIdentifier.getLocalAssigningAuthority(),
        patientIdentifier.getLocalExtenstion(), assertion, false), HttpStatus.OK);
  }

  @PostMapping("/communityIdentifier/{communityIdentifier}/uuid/{uuid}")
  @Operation(description = "Update an entry")
  public ResponseEntity<T> update(
      HttpServletRequest request,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody T newDTO) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    T result = service.update(communityIdentifier, patientIdentifier.getLocalAssigningAuthority(),
        patientIdentifier.getLocalExtenstion(), uuid, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/validate/communityIdentifier/{communityIdentifier}/uuid/{uuid}")
  @Operation(description = "Validate an entry")
  public ResponseEntity<T> validate(
      HttpServletRequest request,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody T newDTO) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    PatientIdentifier patientIdentifier = HttpSessionUtils.getPatientIdentifierFromSession();
    T result = service.validate(communityIdentifier, patientIdentifier.getLocalAssigningAuthority(),
        patientIdentifier.getLocalExtenstion(), uuid, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
