package ch.admin.bag.vaccination.service.saml.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:./config/idp-config-local.yml")
class ServiceProviderTest {

  @Autowired
  private ServiceProvider sp;

  @Test
  void serviceProvider_hasDefaultSamlKeystoreValuesSet() {
    assertThat(sp.getAssertionConsumerServiceUrl()).isNotNull();
    assertThat(sp.getAssertionConsumerServiceUrl()).isEqualTo("http://localhost:8080/saml/sso");
    assertThat(sp.getForwardArtifactToClientUrl()).isNotNull();
    assertThat(sp.getForwardArtifactToClientUrl()).isEqualTo("http://localhost:9000/saml-acs");
    assertThat(sp.getSamlKeystorePath()).isNotNull();
    assertThat(sp.getSamlKeystorePath()).isEqualTo("config/keystore-idp.p12");
    assertThat(sp.getSamlKeystoreType()).isNotNull();
    assertThat(sp.getSamlKeystoreType()).isEqualTo("PKCS12");
    assertThat(sp.getSamlKeystorePassword()).isNotNull();
    assertThat(sp.getSamlSpAlias()).isNotNull();
    assertThat(sp.getSamlSpAlias()).isEqualTo("spkey");
  }

  @Test
  void serviceProvider_whenNoTlsPropertiesAreProvided_nullValuesAreExpected() {
    assertThat(sp.getTlsKeystorePath()).isNull();
    assertThat(sp.getTlsKeystoreType()).isNull();
    assertThat(sp.getTlsKeystorePassword()).isNull();
  }
}