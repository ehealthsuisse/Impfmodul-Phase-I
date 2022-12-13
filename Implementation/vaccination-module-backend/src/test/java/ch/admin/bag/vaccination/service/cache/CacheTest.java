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
package ch.admin.bag.vaccination.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * Test {@Cache}
 *
 */
@SpringBootTest
@ActiveProfiles("test")
public class CacheTest {
  @Autowired
  private Cache cache;

  @Test
  public void clear_anyData_mapIsCleared() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);
    cache.putData(patientIdentifier, "test");
    cache.clear();

    assertThat(cache.getData(patientIdentifier)).isEmpty();
    assertNull(cache.getPatientIdentifier("communityIdentifier", "localId", "oid"));
  }

  @Test
  public void test_getAfterTTL() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);
    assertThat(cache.getPatientIdentifier("communityIdentifier", "oid", "localId")).isNotNull();
    Thread.sleep(2100); // > 2s TTL
    patientIdentifier = cache.getPatientIdentifier("communityIdentifier", "oid", "localId");
    assertThat(patientIdentifier).isNull();
  }

  @Test
  public void test_getBeforeTTL() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);

    patientIdentifier = cache.getPatientIdentifier("communityIdentifier", "oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA");
  }

  @Test
  public void test_getJsonsAfterTTL() throws Exception {
    cache.putData(new PatientIdentifier("communityIdentifier", "localId", "oid"), "json3");
    Thread.sleep(2100); // > 2s TTL
    assertThat(cache.getData(new PatientIdentifier("communityIdentifier", "localId", "oid")).size()).isEqualTo(0);
    assertThat(cache.dataCacheMiss(new PatientIdentifier("communityIdentifier", "localId", "oid"))).isTrue();
  }

  @Test
  public void test_getJsonsBeforeTTL() throws Exception {
    assertThat(cache.getData(new PatientIdentifier("communityIdentifier", "localId", "oid")).size()).isEqualTo(0);
    assertThat(cache.dataCacheMiss(new PatientIdentifier("communityIdentifier", "localId", "oid"))).isTrue();
    cache.putData(new PatientIdentifier("communityIdentifier", "localId", "oid"), "json1");
    assertThat(cache.getData(new PatientIdentifier("communityIdentifier", "localId", "oid")).size()).isEqualTo(1);
    assertThat(cache.dataCacheMiss(new PatientIdentifier("communityIdentifier", "localId", "oid"))).isFalse();
    cache.putData(new PatientIdentifier("communityIdentifier", "localId", "oid"), "json2");
    assertThat(cache.getData(new PatientIdentifier("communityIdentifier", "localId", "oid")).size()).isEqualTo(2);
  }

  @Test
  public void test_PatientIdentifierKey() {
    PatientIdentifierKey key1 = new PatientIdentifierKey(new PatientIdentifier("community", "localId", "oid"));
    assertThat(key1.getCommunityIdentifier()).isEqualTo("community");
    assertThat(key1.getOid()).isEqualTo("oid");
    assertThat(key1.getLocalId()).isEqualTo("localId");
    PatientIdentifierKey key2 = new PatientIdentifierKey(new PatientIdentifier("community", "localId", "oid"));
    assertThat(key1.equals(key2)).isTrue();
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    key2.setLocalId("_localId");
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
  }

  @Test
  public void test_PatientIdentifier() {
    HumanNameDTO patient = new HumanNameDTO("firstName", "lastName", "prefix", LocalDate.now(), "gender");

    PatientIdentifier pid1 = new PatientIdentifier("community", "localId", "oid");
    pid1.setPatientInfo(patient);
    assertThat(pid1.getCommunityIdentifier()).isEqualTo("community");
    assertThat(pid1.getLocalAssigningAuthority()).isEqualTo("oid");
    assertThat(pid1.getLocalExtenstion()).isEqualTo("localId");
    assertThat(pid1.getSpidRootAuthority()).isEqualTo("2.16.756.5.30.1.127.3.10.3");
    assertThat(pid1.getPatientInfo().getPrefix()).isEqualTo("prefix");
    assertThat(pid1.getSpidExtension()).isNull();
    assertThat(pid1.getGlobalAuthority()).isNull();
    assertThat(pid1.getGlobalExtension()).isNull();
    assertThat(pid1.toString()).contains("firstName", "2.16.756.5.30.1.127.3.10.3");
    PatientIdentifier pid2 = new PatientIdentifier("community", "localId", "oid");
    pid2.setPatientInfo(patient);
    assertThat(pid1.equals(pid2)).isTrue();
    assertThat(pid1.hashCode()).isEqualTo(pid2.hashCode());
    pid2.setGlobalAuthority("gloAuth");
    pid2.setGlobalExtension("gloExt");
    pid2.setSpidExtension("spidExt");
    assertThat(pid2.getGlobalAuthority()).isEqualTo("gloAuth");
    assertThat(pid2.getGlobalExtension()).isEqualTo("gloExt");
    assertThat(pid2.getSpidExtension()).isEqualTo("spidExt");
    assertThat(pid1.equals(pid2)).isFalse();
    assertThat(pid1.hashCode()).isNotEqualTo(pid2.hashCode());
  }

  @Test
  public void test_replace() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);

    patientIdentifier = cache.getPatientIdentifier("communityIdentifier", "oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA");


    PatientIdentifier patientIdentifier2 = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier2.setSpidRootAuthority("SRA2");
    cache.putPatientIdentifier(patientIdentifier2);

    patientIdentifier = cache.getPatientIdentifier("communityIdentifier", "oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA2");
  }
}
