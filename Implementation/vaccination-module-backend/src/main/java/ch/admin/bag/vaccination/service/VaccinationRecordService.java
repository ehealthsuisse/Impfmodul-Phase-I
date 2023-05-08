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

import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import org.hl7.fhir.r4.model.Bundle;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * VaccinationRecordService contains all the business logic to handle a {@link VaccinationRecordDTO}
 * or rather the list of vaccinations, allergies and past illnesses.
 *
 */
@Service
public class VaccinationRecordService {
  @Autowired
  protected HuskyAdapterIfc huskyAdapter;

  @Autowired
  protected FhirAdapterIfc fhirAdapter;

  /**
   * Create a json of a {@link VaccinationRecordDTO}
   *
   * @param patientIdentifier the PatientIdentifier
   * @param record the VaccinationRecord
   * @return the json
   */
  public String create(PatientIdentifier patientIdentifier, VaccinationRecordDTO record) {
    Bundle bundle = fhirAdapter.create(patientIdentifier, record);
    String json = fhirAdapter.convertBundleToJson(bundle);
    return json;
  }

  /**
   * Creates a vaccination record in the EPD containing the given vaccinations, allergies and past
   * illnesses.
   *
   * @param communityIdentifier EPD community to talk with
   * @param oid local assigning authority oid
   * @param localId local patient id
   * @param record {@link VaccinationRecordDTO} containing the entities to export
   * @param assertion IDP Assertion
   *
   * @return json containing the full bundle which was written.
   */
  public String create(String communityIdentifier, String oid, String localId, VaccinationRecordDTO record,
      Assertion assertion) {
    PatientIdentifier patientIdentifier = huskyAdapter.getPatientIdentifier(communityIdentifier, oid, localId);

    Bundle bundle = fhirAdapter.create(patientIdentifier, record);
    String jsonToWrite = fhirAdapter.convertBundleToJson(bundle);

    huskyAdapter.writeDocument(patientIdentifier, bundle.getIdentifier().getValue(), jsonToWrite, record.getAuthor(),
        record.getConfidentiality(), assertion);

    return jsonToWrite;
  }

}
