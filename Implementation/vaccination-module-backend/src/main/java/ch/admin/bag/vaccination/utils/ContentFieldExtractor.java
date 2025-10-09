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
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Utility class for extracting field values from various content types (e.g. JSON, XML).
 */
public class ContentFieldExtractor {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Extracts the value of a given field from a JSON/XML string.
   *
   * @param content data in JSON or XML format
   * @param fieldName the name of the field to extract
   * @return the value of the specified field as a String
   * @throws TechnicalException if the JSON/XML parsing fails or the field is missing or null
   */
  public static String extractField(String content, String fieldName) {
    String contentType = "";
    try {
      if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
        contentType = "JSON";
        return extractFromJson(content, fieldName);
      } else {
        contentType = "XML";
        return extractFromXml(content, fieldName);
      }
    } catch (Exception e) {
      throw new TechnicalException("Failed to extract timestamp from " + contentType, e);
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

  private static String extractFromJson(String jsonString, String fieldName) throws Exception {
    JsonNode rootNode = objectMapper.readTree(jsonString);
    JsonNode fieldNode = rootNode.get(fieldName);

    if (fieldNode == null || fieldNode.isNull()) {
      throw new TechnicalException("Field '" + fieldName + "' not found or is null in JSON.");
    }

    return fieldNode.asText();
  }

  private static String extractFromXml(String xmlString, String fieldName) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(xmlString)));

    NodeList nodeList = document.getElementsByTagName(fieldName);

    if (nodeList.getLength() == 0) {
      throw new TechnicalException("Field '" + fieldName + "' not found in XML.");
    }

    Node node = nodeList.item(0);
    String textContent = node.getTextContent();

    if (textContent != null && !textContent.trim().isEmpty()) {
      return textContent.trim();
    }

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      String attributeValue = element.getAttribute("value");
      if (attributeValue != null && !attributeValue.trim().isEmpty()) {
        return attributeValue.trim();
      }
    }

    throw new TechnicalException("Field '" + fieldName + "' has no text content or 'value' attribute in XML.");
  }
}
