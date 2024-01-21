package ch.admin.bag.vaccination.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Ignore SSLValidation
 */
public class SSLValidation {

  /** Ignore Certificate checks */
  public static void disableChecks() throws Exception {
    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] arg0, String arg1)
              throws CertificateException {}

          @Override
          public void checkServerTrusted(X509Certificate[] arg0, String arg1)
              throws CertificateException {}

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
        }
    };

    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    // Create all-trusting host name verifier
    HostnameVerifier validHosts = new HostnameVerifier() {
      @Override
      public boolean verify(String arg0, SSLSession arg1) {
        return true;
      }
    };
    // All hosts will be valid
    HttpsURLConnection.setDefaultHostnameVerifier(validHosts);
  }

}
