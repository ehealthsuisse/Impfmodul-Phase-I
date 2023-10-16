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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorDTOTest {

  private AuthorDTO author;

  @BeforeEach
  void setUp() {
    // Initialize AuthorDTO with some default values for testing
    author = new AuthorDTO(new HumanNameDTO("John", "Doe", "Dr.", null, null), "Role", "GLN");
  }

  @Test
  void testConstructors() {
    // Test the constructor with full parameters
    assertEquals("Dr. John Doe", author.getFullName());
    assertEquals("Role", author.getRole());
    assertEquals("GLN", author.getGln());

    // Test the constructor with only user, other fields should be null
    AuthorDTO authorWithUser = new AuthorDTO(new HumanNameDTO("Jane", "Smith", "Dr.", null, null));
    assertEquals("Dr. Jane Smith", authorWithUser.getFullName());
    assertNull(authorWithUser.getRole());
    assertNull(authorWithUser.getGln());
  }

  @Test
  void testGetFullName() {
    // Test getFullName() when user is not null
    assertEquals("Dr. John Doe", author.getFullName());

    // Test getFullName() when user is null
    author.setUser(null);
    assertEquals("", author.getFullName());
  }

  @Test
  void testSetters() {
    // Test setters for organisation, role, and gln
    author.setOrganisation("Org");
    author.setRole("NewRole");
    author.setGln("NewGLN");

    assertEquals("Org", author.getOrganisation());
    assertEquals("NewRole", author.getRole());
    assertEquals("NewGLN", author.getGln());
  }
}
