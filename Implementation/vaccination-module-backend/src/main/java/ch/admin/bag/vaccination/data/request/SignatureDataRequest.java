/**
 * Copyright (c) 2026 eHealth Suisse
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

import ch.admin.bag.vaccination.controller.PortalController;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Request object for {@link PortalController#validatePortalCall(HttpServletRequest, SignatureDataRequest)}.
 */
public record SignatureDataRequest(
	@Schema(description = "Signed query string received from the calling frontend.",
            example = "idp=GAZELLE&purpose=NORM&sig=abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    String queryString,
	@Schema(description = "Frontend host of the requesting application so the SAML response can be returned to the correct frontend.",
		example = "develop-vaccination-module.apps.ocp4.innershift.sodigital.io")
    String frontendHost) {
}
