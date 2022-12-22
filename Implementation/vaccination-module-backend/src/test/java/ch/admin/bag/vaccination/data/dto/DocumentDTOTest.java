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
package ch.admin.bag.vaccination.data.dto;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;

/** Simple Test class for DTOs */
class DocumentDTOTest {

  private static final String MIMETYPE = "XML";
  private static final String NEW_REPOSITORY_UNIQUE_ID = "repository unique id";
  private static final String NEW_DOCUMENT_UNIQUE_ID = "document unique id";
  private static final String DOCUMENT_CONTENT = " documentContent";

  private DocumentDTO dto;
  private RetrievedDocument documentMock;

  @BeforeEach
  void beforeEach() {
    documentMock = mock(RetrievedDocument.class);
    when(documentMock.getMimeType()).thenReturn(MIMETYPE);
    when(documentMock.getNewDocumentUniqueId()).thenReturn(NEW_DOCUMENT_UNIQUE_ID);
    when(documentMock.getNewRepositoryUniqueId()).thenReturn(NEW_REPOSITORY_UNIQUE_ID);
  }

  @Test
  void constructor_validInput_noException() {
    dto = new DocumentDTO(documentMock, DOCUMENT_CONTENT);

    assertThat(dto.getMimeType()).isEqualTo(MIMETYPE);
    assertThat(dto.getNewDocumentUniqueId()).isEqualTo(NEW_DOCUMENT_UNIQUE_ID);
    assertThat(dto.getNewRepositoryUniqueId()).isEqualTo(NEW_REPOSITORY_UNIQUE_ID);
    assertThat(dto.getDocumentContent()).isEqualTo(DOCUMENT_CONTENT);
  }

  @Test
  void toString_validMessage_returnCorrectFormatting() {
    dto = new DocumentDTO(documentMock, DOCUMENT_CONTENT);

    assertThat(dto.toString()).hasToString(
        "DocumentDTO(mimeType=" + MIMETYPE + ", newRepositoryUniqueId=" + NEW_REPOSITORY_UNIQUE_ID
            + ", newDocumentUniqueId=" + NEW_DOCUMENT_UNIQUE_ID
            + ", documentContent=" + DOCUMENT_CONTENT + ")");
  }

}
