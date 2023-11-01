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
package ch.admin.bag.vaccination.service;

import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.List;
import org.projecthusky.xua.saml2.Assertion;

/**
 *
 * Generic interface for all the DTOs: VaccinationDTO, AllergyDTO, PastIllnessDTO
 *
 */
public interface BaseServiceIfc<T> {

  public T create(String communityIdentifier, String oid, String localId,
      T newDto, Assertion assertion);

  public T delete(String commununityIdentifier, String oid, String localId,
      String uuid, ValueDTO confidentiality, Assertion assertion);

  public List<T> getAll(PatientIdentifier patientIdentifier, Assertion assertion, boolean isLifecycleActive);

  public List<T> getAll(String commununityIdentifier, String oid, String localId, Assertion assertion);

  public List<T> getAll(String commununityIdentifier, String oid,
      String localId, Assertion assertion, boolean isLifecycleActive);

  public T update(String communityIdentifier, String oid, String localId,
      String uuid, T newDto, Assertion assertion);

  public T validate(String communityIdentifier, String oid, String localId,
      String toUpdateUuid, T newDto, Assertion assertion);
}
