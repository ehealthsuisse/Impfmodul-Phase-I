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
package ch.admin.bag.vaccination.service.husky.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bag.vaccination.service.VaccinationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VaccinationConfigTest {
  @Autowired
  private CommunitiesConfig communitiesConfig;
  @Autowired
  private SenderConfig senderConfig;
  @Autowired
  private VaccinationConfig vaccinationConfig;

  @Test
  void codesConfig() {
    assertThat(vaccinationConfig.getDoctor().getHealthCareFacilityTypeCode().getCode()).isEqualTo("43741000");
    assertThat(vaccinationConfig.getDoctor().getHealthCareFacilityTypeCode().getDisplayName())
        .isEqualTo("Site of Care (environment)");
    assertThat(vaccinationConfig.getDoctor().getHealthCareFacilityTypeCode().getCodeSystem())
        .isEqualTo("2.16.840.1.113883.6.96");

    assertThat(vaccinationConfig.getPatient().getHealthCareFacilityTypeCode().getCode()).isEqualTo("66280005");
    assertThat(vaccinationConfig.getPatient().getHealthCareFacilityTypeCode().getDisplayName())
        .isEqualTo("Private home-based care (environment)");
    assertThat(vaccinationConfig.getPatient().getHealthCareFacilityTypeCode().getCodeSystem())
        .isEqualTo("2.16.840.1.113883.6.96");

    assertThat(vaccinationConfig.getDocumentType()).isEqualTo("41000179103");
    assertThat(vaccinationConfig.getSystemIdentifier()).isEqualTo("urn:ietf:rfc:3986");
  }

  @Test
  void communitiesConfig() {
    assertThat(communitiesConfig).isNotNull();
    assertThat(communitiesConfig.getCommunities()).isNotNull();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.UNKNOWN.name())).isNull();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.DUMMY.name())).isNotNull();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())).isNotNull();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name()).getIdentifier())
        .isEqualTo(EPDCommunity.GAZELLE.name());
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getGlobalAssigningAuthorityNamespace())
            .isEqualTo("CHPAM2");
    assertThat(
        communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
            .getGlobalAssigningAuthorityOid())
                .isEqualTo("1.3.6.1.4.1.12559.11.20.1");
  }

  @Test
  void repositoryConfig() {
    assertThat(
        communitiesConfig.getCommunities().getFirst().getRepositoryConfig(EPDCommunity.DUMMY.name()))
            .isNull();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.PDQ.name()).getIdentifier())
            .isEqualTo(EPDRepository.PDQ.name());
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.PDQ.name()).getUri()).isEqualTo(
            "https://ehealthsuisse.ihe-europe.net/PAMSimulator-ejb/PDQSupplier_Service/PDQSupplier_PortType");

    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.PDQ.name()).getReceiver()
        .getApplicationOid()).isEqualTo("2.16.840.1.113883.3.72.6.5.100.1399");
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.PDQ.name()).getReceiver()
        .getFacilityOid()).isEmpty();
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getSpidEprOid()).isEqualTo("2.16.756.5.30.1.127.3.10.3");
    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getSpidEprNamespace()).isEqualTo("SPID");
  }

  @Test
  void senderConfig() {
    assertThat(senderConfig).isNotNull();
    assertThat(senderConfig.getSender()).isNotNull();
    assertThat(senderConfig.getSender().getApplicationOid()).isEqualTo("1.2.3.4");
  }

  @Test
  void xuaConfig() {
    XuaConfig xuaConfig = communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.XUA.name()).getXua();
    assertThat(xuaConfig).isNotNull();
    assertThat(xuaConfig.getClientKeyStoreType()).isEqualTo("PKCS12");
  }
}
