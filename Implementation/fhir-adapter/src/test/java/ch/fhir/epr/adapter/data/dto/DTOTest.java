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

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class DTOTest {
  @Test
  void testAuthorDTO() {
    AuthorDTO author =
        new AuthorDTO(
            new HumanNameDTO("firstName", "lastName", "prefix", null, "gender"),
            "organisation", "role", "purpose", "gln", "principalId", "principalName");

    assertEquals(author.getUser().getFirstName(), "firstName");
    assertEquals(author.getUser().getLastName(), "lastName");
    assertEquals(author.getUser().getPrefix(), "prefix");
    assertEquals(author.getOrganisation(), "organisation");
    assertEquals(author.getRole(), "role");
    assertEquals(author.getPurpose(), "purpose");
    assertEquals(author.getGln(), "gln");
    assertEquals(author.getPrincipalId(), "principalId");
    assertEquals(author.getPrincipalName(), "principalName");
  }

  @Test
  void testHumanNameDTOConstructor() {
    LocalDate now = LocalDate.now();
    HumanNameDTO humanName = new HumanNameDTO("firstName", "lastName", "prefix", now, "gender");
    assertEquals(humanName.getFirstName(), "firstName");
    assertEquals(humanName.getLastName(), "lastName");
    assertEquals(humanName.getPrefix(), "prefix");
    assertEquals(humanName.getBirthday(), now);
    assertEquals(humanName.getGender(), "gender");
  }

  @Test
  void testHumanNameDTOSetter() {
    LocalDate now = LocalDate.now();
    HumanNameDTO humanName = new HumanNameDTO();
    humanName.setFirstName("firstName");
    humanName.setLastName("lastName");
    humanName.setPrefix("prefix");
    humanName.setBirthday(now);
    humanName.setGender("gender");

    assertEquals(humanName.getFirstName(), "firstName");
    assertEquals(humanName.getLastName(), "lastName");
    assertEquals(humanName.getPrefix(), "prefix");
    assertEquals(humanName.getBirthday(), now);
    assertEquals(humanName.getGender(), "gender");
  }
}
