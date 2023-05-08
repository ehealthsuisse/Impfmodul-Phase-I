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
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

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

  @PostMapping("/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}")
  @Operation(description = "Create an entry",
      parameters = {
          @Parameter(in = ParameterIn.HEADER, name = "ufname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "utitle", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "role", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "purpose", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugln", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalId", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalName", required = false) //
      })
  public ResponseEntity<T> create(
      @RequestHeader HttpHeaders headers,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId,
      @RequestBody T newDTO) {
    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);
    newDTO.setAuthor(author);
    Assertion assertion = AssertionUtils.getAssertionFromSession();

    T result = service.create(communityIdentifier, oid, localId, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @DeleteMapping("/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}/uuid/{uuid}")
  @Operation(description = "Delete an entry",
      parameters = {
          @Parameter(in = ParameterIn.HEADER, name = "ufname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "utitle", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "role", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "purpose", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugln", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalId", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalName", required = false) //
      })
  public ResponseEntity<T> delete(
      @RequestHeader HttpHeaders headers,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody ValueDTO confidentiality) {
    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    T result = service.delete(communityIdentifier, oid, localId, uuid, confidentiality, author, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @GetMapping("/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}")
  @Operation(summary = "Get the data of the entries",
      parameters = {
          @Parameter(in = ParameterIn.HEADER, name = "ufname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "utitle", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "role", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "purpose", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugln", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalId", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalName", required = false) //
      })
  public ResponseEntity<List<T>> getAll(
      @RequestHeader HttpHeaders headers,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId) {

    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    return new ResponseEntity<>(
        service.getAll(communityIdentifier, oid, localId, author, assertion, false),
        HttpStatus.OK);
  }

  @PostMapping("/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}/uuid/{uuid}")
  @Operation(description = "Update an entry",
      parameters = {
          @Parameter(in = ParameterIn.HEADER, name = "ufname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "utitle", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "role", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "purpose", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugln", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalId", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalName", required = false) //
      })
  public ResponseEntity<T> update(
      @RequestHeader HttpHeaders headers,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody T newDTO) {

    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);
    newDTO.setAuthor(author);
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    T result = service.update(communityIdentifier, oid, localId, uuid, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/validate/communityIdentifier/{communityIdentifier}/oid/{oid}/localId/{localId}/uuid/{uuid}")
  @Operation(description = "Validate an entry",
      parameters = {
          @Parameter(in = ParameterIn.HEADER, name = "ufname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugname", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "utitle", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "lang", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "role", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "purpose", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "ugln", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalId", required = false), //
          @Parameter(in = ParameterIn.HEADER, name = "principalName", required = false) //
      })
  public ResponseEntity<T> validate(
      @RequestHeader HttpHeaders headers,
      @Schema(example = "GAZELLE") @PathVariable String communityIdentifier,
      @Schema(example = "1.3.6.1.4.1.21367.13.20.3000") @PathVariable String oid,
      @Schema(example = "CHPAM4489") @PathVariable String localId,
      @Schema(example = "uuu-uuu-iii-ddd") @PathVariable String uuid,
      @RequestBody T newDTO) {
    Assertion assertion = AssertionUtils.getAssertionFromSession();
    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);
    newDTO.setAuthor(author);
    T result = service.validate(communityIdentifier, oid, localId, uuid, newDTO, assertion);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
