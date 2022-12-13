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

import ch.fhir.epr.adapter.data.PatientIdentifier;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
  @Autowired
  private CacheConfig cacheConfig;

  public void clear() {
    hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME).clear();
    hazelcast.getMap(DOCUMENT_CACHE_NAME).clear();
  }

  public boolean dataCacheMiss(PatientIdentifier patientIdentifier) {
    PatientIdentifierKey key = new PatientIdentifierKey(patientIdentifier);
    Map<PatientIdentifierKey, List<String>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    boolean miss = !map.containsKey(key);
    log.debug("dataCacheMiss {} {}", key, miss);
    return miss;
  }

  public List<String> getData(PatientIdentifier patientIdentifier) {
    PatientIdentifierKey key = new PatientIdentifierKey(patientIdentifier);
    Map<PatientIdentifierKey, List<String>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    List<String> jsons = getData(map, key);
    log.debug("getJsons {} {}", key, jsons.size());
    return jsons;
  }

  /**
   * Gets a {@link PatientIdentifier} from the cache
   *
   * @param communityIdentifier The community
   * @param oid The assigning community
   * @param localId the local ID of the Patient
   * @return the {@link PatientIdentifier}
   */
  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
    PatientIdentifierKey key = new PatientIdentifierKey(communityIdentifier, oid, localId);
    log.debug("get {}", key);
    Map<PatientIdentifierKey, PatientIdentifier> map = hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME);
    PatientIdentifier PatientIdentifier = map.get(key);
    return PatientIdentifier;
  }

  public void putData(PatientIdentifier patientIdentifier, String json) {
    PatientIdentifierKey key = new PatientIdentifierKey(patientIdentifier);

    Map<PatientIdentifierKey, List<String>> map = hazelcast.getMap(DOCUMENT_CACHE_NAME);
    List<String> jsons = getData(map, key);
    jsons.add(json);
    log.debug("putJson {} {}", key, jsons.size());
    map.put(key, jsons);
  }

  /**
   * Puts the {@link PatientIdentifier} into the cache
   *
   * @param patientIdentifier The {@link PatientIdentifier}
   */
  public void putPatientIdentifier(PatientIdentifier patientIdentifier) {
    Map<PatientIdentifierKey, PatientIdentifier> map = hazelcast.getMap(PATIENT_IDENTIFIER_CACHE_NAME);
    PatientIdentifierKey key = new PatientIdentifierKey(patientIdentifier);
    log.debug("put {}", key);
    map.put(key, patientIdentifier);
  }

  private List<String> getData(Map<PatientIdentifierKey, List<String>> map, PatientIdentifierKey key) {
    List<String> jsons = map.get(key);
    if (jsons == null) {
      jsons = new ArrayList<>();
    }
    return jsons;
  }

  @PostConstruct
  private void init() {
    Config config = new Config();
    config.setClusterName(cacheConfig.getClusterName());
    MapConfig patientMapConfig = config.getMapConfig(PATIENT_IDENTIFIER_CACHE_NAME);
    patientMapConfig
        .setBackupCount(0)
        .setTimeToLiveSeconds(cacheConfig.getTimeToLiveSeconds());
    MapConfig documentMapConfig = config.getMapConfig(DOCUMENT_CACHE_NAME);
    documentMapConfig
        .setBackupCount(0)
        .setTimeToLiveSeconds(cacheConfig.getTimeToLiveSeconds());
    hazelcast = Hazelcast.newHazelcastInstance(config);;
  }
}
