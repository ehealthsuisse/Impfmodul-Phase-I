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

import ch.fhir.epr.adapter.exception.TechnicalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Utility class for extracting field values from JSON strings.
 */
public class JsonFieldExtractor {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Extracts the value of a given field from a JSON string.
   *
   * @param jsonString the JSON string containing the field
   * @param fieldName the name of the field to extract
   * @return the value of the specified field as a String
   * @throws TechnicalException if the JSON parsing fails or the field is missing or null
   */
  public static String extractField(String jsonString, String fieldName) {
    try {
      JsonNode rootNode = objectMapper.readTree(jsonString);
      JsonNode fieldNode  = rootNode.get(fieldName);

      if (fieldNode == null || fieldNode.isNull()) {
        throw new IllegalArgumentException("Field '" + fieldName + "' not found or is null.");
      }

      return fieldNode.asText();
    } catch (Exception e) {
      throw new TechnicalException("Failed to extract timestamp from JSON", e);
    }
  }

  /**
   * Extracts the value of a given field from a JSON string and parses it into a {@link LocalDateTime}.
   * Assumes that the field contains an ISO-8601 date-time string (with or without offset).
   *
   * @param jsonString the JSON string containing the field
   * @param fieldName the name of the field to extract
   * @return the {@link LocalDateTime} parsed from the field value
   * @throws TechnicalException if parsing fails
   */
  public static LocalDateTime extractFieldAsLocalDateTime(String jsonString, String fieldName) {
    String fieldValue = extractField(jsonString, fieldName);
    try {
      OffsetDateTime offsetDateTime = OffsetDateTime.parse(fieldValue);
      return offsetDateTime.toLocalDateTime();
    } catch (DateTimeParseException e) {
      throw new TechnicalException("Failed to parse field '" + fieldName + "' as LocalDateTime", e);
    }
  }
}
