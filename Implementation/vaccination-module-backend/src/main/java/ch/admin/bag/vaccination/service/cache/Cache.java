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

import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The cache of the Impfmodul.
 *
 */
@Service
@Slf4j
public class Cache {
  private String PATIENT_IDENTIFIER_CACHE_NAME = "patient-identifier";
  private String DOCUMENT_CACHE_NAME = "document";
  private HazelcastInstance hazelcast;

  @Value("${epdbackend.cache.enabled:false}")
  private boolean isCacheEnabled;
  @Value("${epdbackend.cache.clustername:vaccination-module-cache}")
  private String clustername;
  @Value("${epdbackend.cache.ttlInSeconds:300}")
  private int cacheTTLInSeconds;

  public void clear() {
    log.debug("Clearing both patient identifier and document cache.");
    hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME).clear();
    hazelcast.getMap(DOCUMENT_CACHE_NAME).clear();
  }

  /**
   * Clears caches for a single patient. This is necessary as there might be a quick login/logout on
   * emergency access.
   *
   * @param cacheIdentifier {@link CacheIdentifierKey}
   */
  public void clear(CacheIdentifierKey cacheIdentifier) {
    log.debug("Clearing document cache for {}.", cacheIdentifier);
    Map<CacheIdentifierKey, List<String>> mapDocument = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    mapDocument.remove(cacheIdentifier);
  }

  public boolean dataCacheMiss(CacheIdentifierKey cacheIdentifier) {
    Map<CacheIdentifierKey, List<String>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    boolean miss = !map.containsKey(cacheIdentifier);
    if (!miss) {
      log.debug("Missed cache entry for {}", cacheIdentifier);
    }
    return miss;
  }

  public List<EPRDocument> getData(CacheIdentifierKey identifier) {
    Map<CacheIdentifierKey, List<EPRDocument>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    List<EPRDocument> documents = getData(map, identifier);
    log.debug("Load cache data for {}. Found {} entries", identifier, documents.size());
    return documents;
  }

  public PatientIdentifier getPatientIdentifier(String localAssigningAuthorityId, String localId) {
    CacheIdentifierKey key = new CacheIdentifierKey(localAssigningAuthorityId, localId, null);
    log.debug("Load cache data for {}", key);
    Map<CacheIdentifierKey, PatientIdentifier> map = hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME);
    return map.get(key);
  }

  public boolean isEnabled() {
    return isCacheEnabled;
  }

  public void putData(CacheIdentifierKey cacheIdentifier, EPRDocument document) {
    Map<CacheIdentifierKey, List<EPRDocument>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    List<EPRDocument> eprDocuments = getData(map, cacheIdentifier);
    if (document.getJsonOrXmlFhirContent() != null) {
      eprDocuments.add(document);
    }
    log.debug("Save cache data for {}. Number of entries: {}", cacheIdentifier, eprDocuments.size());
    map.put(cacheIdentifier, eprDocuments);
  }

  public void putPatientIdentifier(PatientIdentifier patientIdentifier) {
    Map<CacheIdentifierKey, PatientIdentifier> map = hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME);
    CacheIdentifierKey key = new CacheIdentifierKey(patientIdentifier, null);
    log.debug("Store patient information in cache {}", key);
    map.put(key, patientIdentifier);
  }

  private List<EPRDocument> getData(Map<CacheIdentifierKey, List<EPRDocument>> map,
      CacheIdentifierKey key) {
    List<EPRDocument> eprDocuments = map.get(key);
    if (eprDocuments == null) {
      eprDocuments = new ArrayList<>();
    }
    return eprDocuments;
  }

  @PostConstruct
  private void init() {
    log.info("Cache enabled: " + (isCacheEnabled));
    Config config = new Config();
    config.setClusterName(clustername);
    MapConfig patientMapConfig = config.getMapConfig(PATIENT_IDENTIFIER_CACHE_NAME);
    patientMapConfig
        .setBackupCount(0)
        .setTimeToLiveSeconds(isCacheEnabled ? cacheTTLInSeconds : 1);
    MapConfig documentMapConfig = config.getMapConfig(DOCUMENT_CACHE_NAME);
    documentMapConfig
        .setBackupCount(0)
        .setTimeToLiveSeconds(isCacheEnabled ? cacheTTLInSeconds : 1);
    hazelcast = Hazelcast.newHazelcastInstance(config);
  }
}
