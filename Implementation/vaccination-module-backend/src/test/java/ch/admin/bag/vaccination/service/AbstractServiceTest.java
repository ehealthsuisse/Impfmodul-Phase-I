package ch.admin.bag.vaccination.service;

import static org.assertj.core.api.Assertions.assertThat;
import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Define the test to apply to the Business Services and Controllers
 * <li>{@link VaccinationDTO}</li>
 * <li>{@link AllergyDTO}</li>
 * <li>{@link PastIllnessDTO}</li>
 * <li>{@link MedicalProblemDTO}</li>
 *
 */
public abstract class AbstractServiceTest {
  @Autowired
  protected ProfileConfig profileConfig;
  @Autowired
  private Cache cache;

  protected AuthorDTO author;
  protected HumanNameDTO performer;

  @BeforeEach
  public void before() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
    cache.clear();
    author = new AuthorDTO(new HumanNameDTO("hor", "Aut", "Dr.", null, null), "HCP", "gln:1.2.3.4");
    performer = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);
  }

  /**
   * Test the creation of a DTO
   */
  abstract public void testCreate();

  abstract public void testDelete();

  abstract public void testGetAll();

  abstract public void testUpdate();

  abstract public void testValidate();

  protected <T extends BaseDTO, S extends BaseService<T>> void validate(T dto, S service, String role,
      String expectedExceptionMessage) {
    AuthorDTO author = new AuthorDTO(null, role, null);
    dto.setAuthor(author);

    try {
      service.validate("dummy", "dummy", "dummy", null, dto, null);
      assertThat(true).isFalse();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(expectedExceptionMessage);
    }
  }
}
