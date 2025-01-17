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

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
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

  @Autowired
  private BaseService<BaseDTO> baseService;

  @Autowired
  private ProfileConfig profileConfig;

  /**
   * Create a json of a {@link VaccinationRecordDTO}
   *
   * @param patientIdentifier the PatientIdentifier
   * @param assertion {@link Assertion}
   * @return {@link VaccinationRecordDTO}
   */
  public VaccinationRecordDTO create(PatientIdentifier patientIdentifier, Assertion assertion) {
    AuthorDTO createAuthor = new AuthorDTO(new HumanNameDTO("generated by", "system", "document",
        LocalDate.of(1900, 1, 1), AdministrativeGender.UNKNOWN.name()));

    List<BaseDTO> entities = baseService.getAll(patientIdentifier, assertion, true);
    List<VaccinationDTO> vaccinations = entities.parallelStream().filter(entity -> entity instanceof VaccinationDTO)
        .map(entity -> (VaccinationDTO) entity).toList();
    List<AllergyDTO> allergies = entities.parallelStream().filter(entity -> entity instanceof AllergyDTO)
        .map(entity -> (AllergyDTO) entity).toList();
    List<MedicalProblemDTO> medicalProblems = entities.parallelStream()
        .filter(entity -> entity instanceof MedicalProblemDTO).map(entity -> (MedicalProblemDTO) entity).toList();
    List<PastIllnessDTO> pastIllnesses = entities.parallelStream().filter(entity -> entity instanceof PastIllnessDTO)
        .map(entity -> (PastIllnessDTO) entity).toList();

    VaccinationRecordDTO record = new VaccinationRecordDTO(createAuthor, patientIdentifier.getPatientInfo(), allergies,
        pastIllnesses, vaccinations, medicalProblems);

    Bundle bundle = fhirAdapter.create(patientIdentifier, record);
    record.setJson(fhirAdapter.convertBundleToJson(bundle));

    return record;
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
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    record.setAuthor(author);

    Bundle bundle = fhirAdapter.create(patientIdentifier, record);
    String jsonToWrite = fhirAdapter.convertBundleToJson(bundle);

    huskyAdapter.writeDocument(patientIdentifier, bundle.getIdentifier().getValue(), jsonToWrite, record, assertion,
        true);

    return jsonToWrite;
  }
}
