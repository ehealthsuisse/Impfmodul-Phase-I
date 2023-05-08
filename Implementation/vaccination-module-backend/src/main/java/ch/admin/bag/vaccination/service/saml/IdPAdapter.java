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
package ch.admin.bag.vaccination.service.saml;

import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import javax.net.ssl.SSLContext;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.TLSSocketFactory;
import org.apache.http.client.HttpClient;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.pipeline.httpclient.BasicHttpClientMessagePipeline;
import org.opensaml.messaging.pipeline.httpclient.HttpClientMessagePipeline;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.security.impl.SAMLOutboundProtocolMessageSigningHandler;
import org.opensaml.saml.saml2.binding.decoding.impl.HttpClientResponseSOAP11Decoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HttpClientRequestSOAP11Encoder;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.security.credential.Credential;
import org.opensaml.soap.client.http.AbstractPipelineHttpSOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Adapter of the IDP interface.
 *
 */
@Component
public class IdPAdapter {

  @Autowired
  private SignatureService signatureService;

  public ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig,
      ArtifactResolve artifactResolve) {
    try {
      MessageContext contextout = new MessageContext();
      contextout.setMessage(artifactResolve);

      setSigningParameters(contextout);

      InOutOperationContext context = new ProfileRequestContext();
      context.setOutboundMessageContext(contextout);

      AbstractPipelineHttpSOAPClient soapClient = new AbstractPipelineHttpSOAPClient() {
        @Override
        protected HttpClientMessagePipeline newPipeline() throws SOAPException {
          HttpClientRequestSOAP11Encoder encoder = new HttpClientRequestSOAP11Encoder();
          HttpClientResponseSOAP11Decoder decoder = new HttpClientResponseSOAP11Decoder();

          BasicHttpClientMessagePipeline pipeline =
              new BasicHttpClientMessagePipeline(encoder, decoder);

          pipeline.setOutboundPayloadHandler(new SAMLOutboundProtocolMessageSigningHandler());
          return pipeline;
        }
      };

      HttpClient httpClient = createHttpClient();
      soapClient.setHttpClient(httpClient);
      soapClient.send(idpConfig.getArtifactResolutionServiceURL(), context);

      return (ArtifactResponse) context.getInboundMessageContext().getMessage();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private HttpClient createHttpClient() throws Exception {
    HttpClientBuilder clientBuilder = new HttpClientBuilder();
    clientBuilder.setTLSSocketFactory(new TLSSocketFactory(SSLContext.getDefault()));
    return clientBuilder.buildClient();
  }

  private Credential getServiceProviderSignatureCredential() {
    return signatureService.getSamlSPCredential();
  }

  private void setSigningParameters(MessageContext contextout) {
    SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();
    signatureSigningParameters
        .setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
    signatureSigningParameters
        .setSigningCredential(getServiceProviderSignatureCredential());
    signatureSigningParameters
        .setSignatureCanonicalizationAlgorithm(
            SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

    SecurityParametersContext securityParametersContext = contextout
        .getSubcontext(SecurityParametersContext.class, true);
    securityParametersContext.setSignatureSigningParameters(signatureSigningParameters);
  }
}
