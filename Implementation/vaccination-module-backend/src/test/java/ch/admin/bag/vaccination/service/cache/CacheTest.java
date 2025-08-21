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

import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 *
 * Test {@Cache}
 *
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheTest {
  @Autowired
  private Cache cache;

  @Test
  void clear_anyData_mapIsCleared() {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    CacheIdentifierKey cacheIdentifier = new CacheIdentifierKey(patientIdentifier, null);
    cache.putPatientIdentifier(patientIdentifier);
    cache.putData(cacheIdentifier, new EPRDocument(false, "test", null, null));
    cache.clear();

    assertThat(cache.getData(cacheIdentifier)).isEmpty();
    assertNull(cache.getPatientIdentifier("localId", "oid"));
  }

  @Test
  void ignoreCache_anyCachedData_isGoneAfter1Second() throws InterruptedException {
    setEnableCacheAndReinit(true);

    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");

    cache.putPatientIdentifier(patientIdentifier);
    assertThat(cache.getPatientIdentifier("oid", "localId")).isNotNull();

    Thread.sleep(2000); // > 1s TTL

    patientIdentifier = cache.getPatientIdentifier("oid", "localId");
    assertThat(patientIdentifier).isNull();
  }

  @BeforeEach
  void setUp() {
    setEnableCacheAndReinit(false);
  }

  @Test
  void test_CacheIdentifierKey() {
    setAuthorInSession(new AuthorDTO(null, null, "123"));
    CacheIdentifierKey key1 = cache.createCacheIdentifier(new PatientIdentifier("community", "localId", "oid"));
    assertThat(key1.getOid()).isEqualTo("oid");
    assertThat(key1.getLocalId()).isEqualTo("localId");
    CacheIdentifierKey key2 =
        cache.createCacheIdentifier(new PatientIdentifier("community", "localId", "oid"));
    assertThat(key1.equals(key2)).isTrue();
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());

    // localId effects key
    key2.setLocalId("_localId");
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    key2.setLocalId("localId");

    // gln effects key
    key2.setGln(null);
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    key2.setGln("123");

    // community does NOT effect key
    CacheIdentifierKey key3 = cache.createCacheIdentifier(new PatientIdentifier("UNKNOWNCOMM", "localId", "oid"));
    assertThat(key1.equals(key3)).isTrue();
    assertThat(key1.hashCode()).isEqualTo(key3.hashCode());
  }

  @Test
  void test_getAfterTTL() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);
    assertThat(cache.getPatientIdentifier("oid", "localId")).isNotNull();
    Thread.sleep(2100); // > 2s TTL
    patientIdentifier = cache.getPatientIdentifier("oid", "localId");
    assertThat(patientIdentifier).isNull();
  }

  @Test
  void test_getBeforeTTL() {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);

    patientIdentifier = cache.getPatientIdentifier("oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA");
  }

  @Test
  void test_getJsonsAfterTTL() throws Exception {
    CacheIdentifierKey cacheIdentifier = new CacheIdentifierKey(
        new PatientIdentifier("communityIdentifier", "localId", "oid"), new AuthorDTO(null, null, "123"));
    cache.putData(cacheIdentifier, new EPRDocument(false, "json3", null, LocalDateTime.now()));
    Thread.sleep(2100); // > 2s TTL
    assertThat(cache.getData(cacheIdentifier).size()).isEqualTo(0);
    assertThat(cache.dataCacheMiss(cacheIdentifier)).isTrue();
  }

  @Test
  void test_vaccinationRecordsCache() throws Exception {
    CacheIdentifierKey cacheIdentifier = new CacheIdentifierKey(
        new PatientIdentifier("communityIdentifier", "localId", "oid"), new AuthorDTO(null, null, "123"));
    cache.putData(cacheIdentifier, new EPRDocument(false, "json3", null, LocalDateTime.now()), Cache.VACCINATION_RECORDS_CACHE_NAME);
    Thread.sleep(2100); // > 2s TTL
    assertThat(cache.getData(cacheIdentifier, Cache.VACCINATION_RECORDS_CACHE_NAME).size()).isEqualTo(1);

    cache.clear(cacheIdentifier, Cache.VACCINATION_RECORDS_CACHE_NAME);
    assertThat(cache.getData(cacheIdentifier, Cache.VACCINATION_RECORDS_CACHE_NAME).size()).isEqualTo(0);
  }

  @Test
  void test_getJsonsBeforeTTL() {
    CacheIdentifierKey cacheIdentifier = new CacheIdentifierKey(
        new PatientIdentifier("communityIdentifier", "localId", "oid"), new AuthorDTO(null, null, "123"));
    assertThat(cache.getData(cacheIdentifier).size()).isEqualTo(0);
    assertThat(cache.dataCacheMiss(cacheIdentifier)).isTrue();
    cache.putData(cacheIdentifier, new EPRDocument(false, "json1", null, LocalDateTime.now()));
    assertThat(cache.getData(cacheIdentifier).size()).isEqualTo(1);
    assertThat(cache.dataCacheMiss(cacheIdentifier)).isFalse();
    cache.putData(cacheIdentifier, new EPRDocument(false, "json2", null, LocalDateTime.now()));
    assertThat(cache.getData(cacheIdentifier).size()).isEqualTo(2);
  }

  @Test
  void test_PatientIdentifier() {
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
  void test_replace() {
    PatientIdentifier patientIdentifier = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier.setSpidRootAuthority("SRA");
    cache.putPatientIdentifier(patientIdentifier);

    patientIdentifier = cache.getPatientIdentifier("oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA");


    PatientIdentifier patientIdentifier2 = new PatientIdentifier("communityIdentifier", "localId", "oid");
    patientIdentifier2.setSpidRootAuthority("SRA2");
    cache.putPatientIdentifier(patientIdentifier2);

    patientIdentifier = cache.getPatientIdentifier("oid", "localId");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("SRA2");
  }

  private void setEnableCacheAndReinit(boolean isCacheEnabled) {
    ReflectionTestUtils.setField(cache, "isCacheEnabled", isCacheEnabled);
    ReflectionTestUtils.invokeMethod(cache, "init");
  }

  private void setAuthorInSession(AuthorDTO author) {
    Objects.nonNull(author);
    HttpSessionUtils.setParameterInSession(HttpSessionUtils.AUTHOR, author);
  }
}
