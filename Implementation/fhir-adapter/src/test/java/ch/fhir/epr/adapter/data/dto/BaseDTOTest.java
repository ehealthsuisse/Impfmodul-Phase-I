/**
 * Copyright (c) 2023 eHealth Suisse
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseDTOTest {

  private BaseDTO dto;

  @Test
  void getterSetter_returnSameValue() {
    AuthorDTO authorMock = mock(AuthorDTO.class);
    ValueDTO valueMock = mock(ValueDTO.class);
    CommentDTO commentMock = mock(CommentDTO.class);
    byte[] contentMock = {};
    LocalDateTime now = LocalDateTime.now();
    HumanNameDTO recorder = mock(HumanNameDTO.class);

    dto.setAuthor(authorMock);
    dto.setCode(valueMock);
    dto.setComment(commentMock);
    dto.setConfidentiality(valueMock);
    dto.setContent(contentMock);
    dto.setCreatedAt(now);
    dto.setDeleted(true);
    dto.setHasErrors(true);
    dto.setId("id");
    dto.setJson("json");
    dto.setOrganization("organization");
    dto.setRecorder(recorder);
    dto.setRelatedId("relatedId");
    dto.setUpdated(true);
    dto.setValidated(true);
    dto.setHasErrors(true);

    assertEquals(authorMock, dto.getAuthor());
    assertEquals(valueMock, dto.getCode());
    assertEquals(commentMock, dto.getComment());
    assertEquals(valueMock, dto.getConfidentiality());
    assertArrayEquals(contentMock, dto.getContent());
    assertEquals(now, dto.getCreatedAt());
    assertTrue(dto.isDeleted());
    assertTrue(dto.isHasErrors());
    assertEquals("id", dto.getId());
    assertEquals("json", dto.getJson());
    assertEquals(recorder, dto.getRecorder());
    assertEquals("organization", dto.getOrganization());
    assertEquals("relatedId", dto.getRelatedId());
    assertTrue(dto.isUpdated());
    assertTrue(dto.isValidated());
    assertTrue(dto.isHasErrors());
    assertNull(dto.getDateOfEvent());
  }

  @BeforeEach
  void setUp() {
    dto = new VaccinationDTO();
  }
}
