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
public class BaseDTO {
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
  private String json;
  private ValueDTO confidentiality;
}
