/**
 * Copyright (c) 2025 eHealth Suisse
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
package ch.admin.bag.vaccination.utils;

import static org.junit.jupiter.api.Assertions.*;

import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContentFieldExtractor}.
 *
 * This test class verifies that the {@code extractTimestamp} method correctly
 * extracts the timestamp from a valid JSON/XML string and properly handles error cases,
 * such as missing fields or invalid formats.
 */
class ContentFieldExtractorTest {
  private static final String TIMESTAMP = "timestamp";
  @Test
  void shouldExtractTimestampFromJsonSuccessfully() {
    String json = "{ \"identifier\": { \"system\": \"ttt\", \"value\": \"ttt.rrr.eee\" }, \"type\": \"document\", \"timestamp\": \"2021-10-06T00:00:00.390+02:00\" }";

    LocalDateTime timestamp = ContentFieldExtractor.extractFieldAsLocalDateTime(json, TIMESTAMP);

    assertEquals(LocalDateTime.of(2021, 10, 6, 0, 0, 0, 390_000_000),
        timestamp);
  }
  @Test
  void shouldExtractTimestampFromXMLWithValueAttribute() {
    String xmlContent = "<root><timestamp value='2023-11-23T16:21:24.202+01:00'/></root>";

    String result = ContentFieldExtractor.extractField(xmlContent, TIMESTAMP);

    assertEquals("2023-11-23T16:21:24.202+01:00", result);
  }

  @Test
  void shouldExtractTimestampFromXmlWithTextContent() {
    String xmlContent = "<root><timestamp>2023-11-23T16:21:24.202+01:00</timestamp></root>";

    String result = ContentFieldExtractor.extractField(xmlContent, TIMESTAMP);

    assertEquals("2023-11-23T16:21:24.202+01:00", result);
  }

  @Test
  void shouldThrowExceptionWhenTimestampMissing() {
    String json = "{ \"identifier\": { \"system\": \"ttt\", \"value\": \"ttt.rrr.eee\" }, \"type\": \"document\" }";

    Exception exception = assertThrows(TechnicalException.class, () -> {
      ContentFieldExtractor.extractField(json, TIMESTAMP);
    });

    assertTrue(exception.getMessage().contains("Failed to extract timestamp from JSON"));
  }

  @Test
  void shouldThrowExceptionForInvalidJson() {
    String invalidJson = "{ \"identifier\": { \"system\": \"ttt\" \"value\": \"ttt.rrr.eee\" }"; // missing comma

    Exception exception = assertThrows(TechnicalException.class, () -> {
      ContentFieldExtractor.extractField(invalidJson, TIMESTAMP);
    });

    assertTrue(exception.getMessage().contains("Failed to extract timestamp from JSON"));
  }

  @Test
  void shouldThrowExceptionForInvalidXML() {
    String xmlContent = "<root><other value='some-value'/></root>";

    TechnicalException exception = assertThrows(TechnicalException.class,
        () -> ContentFieldExtractor.extractField(xmlContent, TIMESTAMP));

    assertTrue(exception.getMessage().contains("Failed to extract timestamp from XML"));
    assertTrue(exception.getCause().getMessage().contains("Field 'timestamp' not found in XML"));
  }

  @Test
  void shouldThrowExceptionWhenXmlHasEmptyTimestampField() {
    String xmlContent = "<root><timestamp/></root>";

    TechnicalException exception = assertThrows(TechnicalException.class,
        () -> ContentFieldExtractor.extractField(xmlContent, TIMESTAMP));

    assertTrue(exception.getMessage().contains("Failed to extract timestamp from XML"));
    assertTrue(exception.getCause().getMessage().contains("Field 'timestamp' has no text content or 'value' attribute in XML"));
  }
}