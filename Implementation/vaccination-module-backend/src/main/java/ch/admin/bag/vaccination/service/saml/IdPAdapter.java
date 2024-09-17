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

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.TLSSocketFactory;
import org.apache.http.client.HttpClient;
import org.apache.tomcat.util.compat.TLS;
import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.core.xml.XMLObject;
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
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.soap.client.http.AbstractPipelineHttpSOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.wstrust.RequestSecurityTokenResponse;
import org.opensaml.soap.wstrust.RequestedSecurityToken;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Adapter of the IDP interface.
 *
 */
@Component
@Slf4j
public class IdPAdapter {

  @Autowired
  private SignatureService signatureService;

  public void addSigningParametersToContext(MessageContext contextout) {
    SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();
    signatureSigningParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
    signatureSigningParameters.setSigningCredential(getServiceProviderSignatureCredential());
    signatureSigningParameters.setSignatureCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

    X509KeyInfoGeneratorFactory x509Factory = new X509KeyInfoGeneratorFactory();
    x509Factory.setEmitEntityCertificate(true);
    KeyInfoGenerator generator = x509Factory.newInstance();
    signatureSigningParameters.setKeyInfoGenerator(generator);

    contextout.getSubcontext(SecurityParametersContext.class, true)
        .setSignatureSigningParameters(signatureSigningParameters);
  }

  public Assertion refreshToken(Assertion assertion, String tokenRenewalURL) {
    if (tokenRenewalURL == null) {
      log.warn("Token could not be refreshed as no security token service url was provided in the config.");
      return assertion;
    }

    Envelope request = AssertionRenewalUtils.createRenewalRequest(assertion,
        (BasicX509Credential) getServiceProviderSignatureCredential());

    MessageContext contextout = new MessageContext();
    contextout.setMessage(request);
    InOutOperationContext context = new ProfileRequestContext();
    context.setOutboundMessageContext(contextout);

    AbstractPipelineHttpSOAPClient soapClient = null;
    try {
      soapClient = AssertionRenewalUtils.createRenewalSoapClient(request, createHttpClient());
      SAMLUtils.logSAMLObject((XMLObject) context.getOutboundMessageContext().getMessage());
      soapClient.send(tokenRenewalURL, context);
      return processResponse(context.getInboundMessageContext().getMessage());
    } catch (Exception e) {
      log.error("Received IDP's error message: " + (e.getMessage() != null ? e.getMessage() : "no message available"));
      if (log.isDebugEnabled()) {
        getInformation(e);
      }
      throw new RuntimeException(e);
    }
  }

  public ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig,
      ArtifactResolve artifactResolve) {
    log.debug("Sending and receiving artifact resolve for idp {}", idpConfig.getIdentifier());
    return sendMessage(idpConfig.getArtifactResolutionServiceURL(), artifactResolve);
  }

  Assertion processResponse(Object response) {
    log.debug("Processing refresh token response.");
    if (response instanceof XMLObject xmlResponse) {
      SAMLUtils.logSAMLObject(xmlResponse);
    }

    if (response instanceof RequestSecurityTokenResponse secTokenResponse) {
      log.debug("Refresh Token Response is instance of RequestSecurityTokenResponse");
      List<XMLObject> tokenReference = secTokenResponse.getUnknownXMLObjects(RequestedSecurityToken.ELEMENT_NAME);
      log.debug("Found {} SecurityTokenReference elements.", tokenReference.size());
      if (!tokenReference.isEmpty() && tokenReference.get(0) instanceof RequestedSecurityToken rst) {
        return (Assertion) rst.getUnknownXMLObject();
      }
    }

    throw new RuntimeException("Refresh failed - expected response to contain a "
        + "RequestSecurityTokenResponse.");
  }

  private HttpClient createHttpClient() throws Exception {
    log.debug("Load the keystore");
    KeyStore keyStore = KeyStore.getInstance(System.getProperty("java.net.ssl.keyStoreType", "PKC12"));
    try (FileInputStream keyStoreStream = new FileInputStream(System.getProperty("java.net.ssl.keyStore"))) {
      keyStore.load(keyStoreStream, System.getProperty("java.net.ssl.keyStorePassword").toCharArray());
    }

    log.debug("Initialize the KeyManagerFactory");
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, System.getProperty("java.net.ssl.keyStorePassword").toCharArray());

    log.debug("Load the truststore");
    KeyStore trustStore = KeyStore.getInstance(System.getProperty("java.net.ssl.trustStoreType", "JKS"));
    try (FileInputStream trustStoreStream = new FileInputStream(System.getProperty("java.net.ssl.trustStore"))) {
      trustStore.load(trustStoreStream, System.getProperty("java.net.ssl.trustStorePassword").toCharArray());
    }

    log.debug("Initialize the TrustManagerFactory");
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);

    log.debug("Initialize the SSLContext");
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

    log.debug("Configure the HttpClientBuilder with the SSLContext");
    HttpClientBuilder clientBuilder = new HttpClientBuilder();
    clientBuilder.setTLSSocketFactory(new TLSSocketFactory(sslContext));
    // clientBuilder.setUseSystemProperties(true);

    log.debug("Build the HttpClient");
    return clientBuilder.buildClient();
  }

  /**
   * Simple soap client which signs request output as designed by the opensaml framework. The simple
   * client cannot be used for renewal request because the object structure does not correspond to
   * opensaml standard!
   */
  private AbstractPipelineHttpSOAPClient createSimpleSoapClient() throws Exception {
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
    return soapClient;
  }

  private void getInformation(Exception e) {
    Throwable detail = e;
    log.debug("Get all error messages during IDP refresh:");
    int i = 1;
    // log maximal 5 inner exceptions
    while (detail.getCause() != null && !detail.equals(detail.getCause()) && i <= 5) {
      detail = e.getCause();
      log.debug(e.getMessage());
      i++;
    }
  }

  private Credential getServiceProviderSignatureCredential() {
    return signatureService.getSamlSPCredential();
  }

  @SuppressWarnings("unchecked")
  private <T> T sendMessage(String destinationUrl, Object messageBody) {
    AbstractPipelineHttpSOAPClient soapClient = null;
    try {
      MessageContext contextout = new MessageContext();
      contextout.setMessage(messageBody);

      addSigningParametersToContext(contextout);

      InOutOperationContext context = new ProfileRequestContext();
      context.setOutboundMessageContext(contextout);

      soapClient = createSimpleSoapClient();
      SAMLUtils.logSAMLObject((XMLObject) context.getOutboundMessageContext().getMessage());
      soapClient.send(destinationUrl, context);

      return (T) context.getInboundMessageContext().getMessage();
    } catch (Exception e) {
      log.error(e.getMessage());
      if (log.isDebugEnabled()) {
        getInformation(e);
      }
      throw new RuntimeException(e);
    }
  }
}
