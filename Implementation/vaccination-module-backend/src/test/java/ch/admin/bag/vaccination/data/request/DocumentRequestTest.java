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
package ch.admin.bag.vaccination.data.request;


import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Simple Test class for DTOs */
class DocumentRequestTest {

  private static final String COMMUNITY_IDENTIFIER = "community identifier";
  private static final String ASSIGNING_AUTHORITY_OID = "assigning authority oid";
  private static final String LOCAL_ID = "local patien id";
  private static final String ENTRY_UUID = "entryUuid";

  private DocumentRequest request;
  private DocumentEntryRequest documentEntryRequest;

  @BeforeEach
  void beforeEach() {
    documentEntryRequest =
        new DocumentEntryRequest(COMMUNITY_IDENTIFIER, ASSIGNING_AUTHORITY_OID, LOCAL_ID);
  }

  @Test
  void constructor_validInput_noException() {
    request = new DocumentRequest(documentEntryRequest, ENTRY_UUID);

    assertThat(request.getDocumentEntryRequest()).isEqualTo(documentEntryRequest);
    assertThat(request.getEntryUuid()).isEqualTo(ENTRY_UUID);
  }

  @Test
  void toString_validMessage_returnCorrectFormatting() {
    request = new DocumentRequest(documentEntryRequest, ENTRY_UUID);

    assertThat(request.toString()).hasToString(
        "DocumentRequest(documentEntryRequest="
            + "DocumentEntryRequest(communityIdentifier=" + COMMUNITY_IDENTIFIER
            + ", localAssigningAuthorityOid=" + ASSIGNING_AUTHORITY_OID
            + ", localId=" + LOCAL_ID + ")"
            + ", entryUuid=" + ENTRY_UUID + ")");
  }

}
