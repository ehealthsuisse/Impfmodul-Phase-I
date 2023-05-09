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
package ch.fhir.epr.adapter.data.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import org.junit.jupiter.api.Test;

public class PatientIdentifierTest {

  @Test
  void testConstructor() {
    PatientIdentifier patientIdentifier =
        new PatientIdentifier("communityIdentifier", "localId", "localAssigningAuthorityId");

    assertEquals(patientIdentifier.getCommunityIdentifier(), "communityIdentifier");
    assertEquals(patientIdentifier.getLocalExtenstion(), "localId");
    assertEquals(patientIdentifier.getLocalAssigningAuthority(), "localAssigningAuthorityId");

    assertEquals(patientIdentifier.getSpidExtension(), null);
    assertEquals(patientIdentifier.getSpidRootAuthority(), "2.16.756.5.30.1.127.3.10.3");
    assertEquals(patientIdentifier.getGlobalExtension(), null);
    assertEquals(patientIdentifier.getGlobalAuthority(), null);
  }

  @Test
  void testEqualsAndHashCode() {
    PatientIdentifier patientIdentifier1 =
        new PatientIdentifier("AAA", "BBB", "CCC");
    PatientIdentifier patientIdentifier2 =
        new PatientIdentifier("AAA", "BBB", "CCx");

    assertFalse(patientIdentifier1.equals(patientIdentifier2));
    assertNotEquals(patientIdentifier1.hashCode(), patientIdentifier2.hashCode());
  }

  @Test
  void testSetter() {
    PatientIdentifier patientIdentifier =
        new PatientIdentifier("communityIdentifier", "localId", "localAssigningAuthorityId");

    patientIdentifier.setSpidExtension("SpidExtension");
    patientIdentifier.setSpidRootAuthority("SpidRootAuthority");
    patientIdentifier.setGlobalExtension("GlobalExtension");
    patientIdentifier.setGlobalAuthority("GlobalAuthority");

    assertEquals(patientIdentifier.getSpidExtension(), "SpidExtension");
    assertEquals(patientIdentifier.getSpidRootAuthority(), "SpidRootAuthority");
    assertEquals(patientIdentifier.getGlobalExtension(), "GlobalExtension");
    assertEquals(patientIdentifier.getGlobalAuthority(), "GlobalAuthority");
  }

  @Test
  void testToString() {
    PatientIdentifier patientIdentifier =
        new PatientIdentifier("AAA", "BBB", "CCC");

    assertTrue(patientIdentifier.toString().contains("AAA"));
    assertTrue(patientIdentifier.toString().contains("BBB"));
    assertTrue(patientIdentifier.toString().contains("CCC"));
  }
}
