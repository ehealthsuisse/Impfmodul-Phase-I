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

import ch.admin.bag.vaccination.service.VaccinationService;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * Vaccination controller of the module
 *
 */
@RestController
@RequestMapping("/vaccination")
@Tag(name = "VaccinationController", description = "Vaccination operation of the application")
public class VaccinationController extends BaseController<VaccinationDTO> {

  public VaccinationController(VaccinationService vaccinationService) {
    super(vaccinationService);
  }

  @PostMapping("/validate/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}/uuid/{uuid}")
  @Operation(description = "Validate a Vaccination entry")
  public ResponseEntity<VaccinationDTO> validate(
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody VaccinationDTO newDTO) {
    Assertion assertion = AssertionUtils.getAssertion();

    VaccinationDTO result = service.validate(communityIdentifier, oid, localId, uuid, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
