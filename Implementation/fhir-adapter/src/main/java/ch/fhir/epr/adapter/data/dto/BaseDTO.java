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

import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.utils.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * Base DTO
 *
 */
@Getter
@Setter
public abstract class BaseDTO {
  /** uuid of the item */
  private String id;
  private ValueDTO code;
  /** entry is deleted */
  private boolean deleted = false;
  /** if not null, this entry refers to an earlier version */
  private String relatedId = null;
  /** if true, there is a newer entry */
  private boolean isUpdated = false;
  /** if true, an HCP or ASS is author of this document */
  private boolean validated;
  /** time of creation in the EPD */
  private LocalDateTime createdAt;
  /** only for modifying - author of the document */
  private AuthorDTO author;
  /** HCP which has made the diagnoses or performed the vaccination = the performer */
  private HumanNameDTO recorder;
  /** organization of the recorder */
  private String organization;
  private List<CommentDTO> comments;
  /** content in case of valid fhir document */
  private String json;
  /** confidentiality of an item, e.g. secret to indicate that it is only visible to the patient */
  private ValueDTO confidentiality = FhirConstants.DEFAULT_CONFIDENTIALITY_CODE;

  // Special attributes not used by this library but useful if a bundle could not be parsed. In this
  // case an error DTO can be created and for example filled by the meta information of a document.
  // DTOs with attribute hasErrorSet are ignored when creating a vaccination record.
  /** indicates that a document could not be parsed */
  private boolean hasErrors = false;
  /** content in case of invalid fhir document */
  private byte[] content;

  /**
   * Hook method for convenience reasons to get both occurence date and recorded date
   *
   * @return day of the diagnosis or the vaccination.
   */
  @JsonIgnore
  public abstract LocalDate getDateOfEvent();

  /**
   * Validates this entity, during read or delete operation, we are giving some more flexibility to
   * what we allow, e.g. persons first or family name needn't be filled.
   *
   * @param isReadOrDelete <code>true</code> if it is a reading or delete operation on a bundle.
   */
  public void validate(boolean isReadOrDelete) {
    ValidationUtils.isValid(this, isReadOrDelete);
  }
}

