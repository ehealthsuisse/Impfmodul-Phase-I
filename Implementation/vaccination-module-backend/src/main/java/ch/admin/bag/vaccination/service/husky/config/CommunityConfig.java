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

import ch.fhir.epr.adapter.exception.TechnicalException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CommunityConfig {
  /** name of the community */
  private String identifier;

  /** Assigning authority's OID (sometimes called root oid) */
  private String globalAssigningAuthorityOid;

  /** Assigning authority's namespace (sometimes called root namespace) */
  private String globalAssigningAuthorityNamespace;

  /** SpidOid - should be the same for all Switzerland */
  private String spidEprOid;

  /** SpidNamespace - can vary by community */
  private String spidEprNamespace;

  /** list of supported repositories used to provide the different EPD functionalities */
  private List<RepositoryConfig> repositories;

  public RepositoryConfig getRepositoryConfig(EPDRepository repository) {
    return getRepositoryConfig(repository.name());
  }

  public RepositoryConfig getRepositoryConfig(String repositoryIdentifier) {
    if (repositories != null) {
      return repositories.stream()
          .filter(config -> config.getIdentifier().equalsIgnoreCase(repositoryIdentifier))
          .findFirst()
          .orElseThrow(() -> {
            log.warn("Repository Config for {} not found", repositoryIdentifier);
            throw new TechnicalException("repository.not.found");
          });
    }

    return null;
  }
}
