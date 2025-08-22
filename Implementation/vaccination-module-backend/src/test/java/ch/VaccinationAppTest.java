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
package ch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.admin.bag.vaccination.service.husky.HuskyAdapter;
import ch.admin.bag.vaccination.service.husky.config.CommunitiesConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.admin.bag.vaccination.service.husky.config.EPDRepository;
import ch.admin.bag.vaccination.service.husky.config.SenderConfig;
import ch.admin.bag.vaccination.service.saml.IdPAdapter;
import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import ch.fhir.epr.adapter.exception.TechnicalException;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.projecthusky.communication.ConvenienceCommunication;
import org.projecthusky.communication.ConvenienceMasterPatientIndexV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
@ActiveProfiles("test")
class VaccinationAppTest {

  @Autowired
  private VaccinationApp vaccinationApp;

  @Autowired
  private CamelContext camelContext;

  @Autowired
  private ConvenienceMasterPatientIndexV3 convenienceMasterPatientIndexV3Client;

  @Autowired
  private ConvenienceCommunication convenienceCommunication;

  @Autowired
  private CommunitiesConfig communitiesConfig;

  @Autowired
  private SenderConfig senderConfig;

  @Autowired
  private HuskyAdapter huskyAdapter;

  @Autowired
  private SAMLServiceIfc samlService;

  @Autowired
  private IdPAdapter idpAdapter;

  @Test
  void communitiesConfig() {
    assertThat(communitiesConfig).isNotNull();
    assertThat(communitiesConfig.getCommunities()).isNotNull();
    assertThat(communitiesConfig.getCommunities().getFirst().getIdentifier())
        .isEqualTo(EPDCommunity.DUMMY.name());

    assertThat(communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDRepository.PDQ.name()).getReceiver()
        .getApplicationOid()).isEqualTo("2.16.840.1.113883.3.72.6.5.100.1399");
    assertThrows(TechnicalException.class, () -> communitiesConfig.getCommunityConfig(EPDCommunity.GAZELLE.name())
        .getRepositoryConfig(EPDCommunity.DUMMY.name()));
  }

  @Test
  void senderConfig() {
    assertThat(senderConfig).isNotNull();
    assertThat(senderConfig.getSender()).isNotNull();
    assertThat(senderConfig.getSender().getApplicationOid()).isEqualTo("1.2.3.4");
  }

  @Test
  void startApp_noExceptionsOccure() {
    assertThat(vaccinationApp).isNotNull();

    assertThat(camelContext).isNotNull();
    assertThat(convenienceMasterPatientIndexV3Client).isNotNull();
    assertThat(convenienceCommunication).isNotNull();

    assertThat(communitiesConfig).isNotNull();
    assertThat(senderConfig).isNotNull();
    assertThat(huskyAdapter).isNotNull();
    assertThat(samlService).isNotNull();
    assertThat(idpAdapter).isNotNull();

    // run explicitely to fulfill coverage criteria
    VaccinationApp.main(new String[] {});
    new VaccinationApp().configure(new SpringApplicationBuilder());
  }

}
