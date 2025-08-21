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
package ch.fhir.epr.adapter.data;

import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Wrapper Object to have all patientIds at one place.
 *
 * <p>
 * Example: For GAZELLE <br>
 * Local ID: 08242eb8-dd47-4298-8d2f-25d60114f137^^^&amp;1.1.1.2.2&amp;ISO <br>
 * SPID: 761337610411353650^^^&amp;2.16.756.5.30.1.127.3.10.3&amp;ISO <br>
 * Global Id: 25f98b34-0e01-48b7-a06c-f706eb4c485f^^^&amp;1.3.6.1.4.1.21367.2017.2.5.93&amp;ISO
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class PatientIdentifier implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  /** Community. Ex: GAZELLE, EPDPLAYGROUND */
  private String communityIdentifier;
  // local ID known only to the local assigning authority
  // Refering to the example 08242eb8-dd47-4298-8d2f-25d60114f137
  private String localExtenstion;

  // Refering to the example 1.1.1.2.2
  private String localAssigningAuthority;

  // Resolved via PIX Query when domain spid root id is given.
  // Refering to the example 761337610411353650
  private String spidExtension;

  // Spid Root Authority Id can be configured per community but is the same all over Switzerland. */
  // Refering to the example 2.16.756.5.30.1.127.3.10.3
  private String spidRootAuthority = "2.16.756.5.30.1.127.3.10.3";

  // Resolved via PIX Query when domain for the global assigning authority is given.
  // Referring to the example 25f98b34-0e01-48b7-a06c-f706eb4c485f
  private String globalExtension;

  // It is sometimes also referred to as masterpatientIndex- or root-Authority
  // and is configured per community.
  // Referring to the example 1.3.6.1.4.1.21367.2017.2.5.93
  private String globalAuthority;

  private HumanNameDTO patientInfo;

  public PatientIdentifier(String communityIdentifier, String localId,
      String localAssigningAuthorityId) {
    this.communityIdentifier = communityIdentifier;
    localExtenstion = localId;
    localAssigningAuthority = localAssigningAuthorityId;
  }

}
