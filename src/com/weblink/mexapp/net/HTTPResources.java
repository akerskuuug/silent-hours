package com.weblink.mexapp.net;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class HTTPResources {

	private static final String QUERY = "Query";
	private static final String RESPONSE = "Response";

	public static String performAction(final User user, final ArrayList<NameValuePair> nvps) {
		return performAction(user.getCompany(), user.getExtension() + "", user.getPassword(), Tools.createJSONObject(nvps).toString());

	}

	public static String performAction(final User user, final JSONArray jData) {
		return performAction(user.getCompany(), user.getExtension() + "", user.getPassword(), jData.toString());

	}

	public static String performAction(final User user, final JSONObject jData) {
		return performAction(user.getCompany(), user.getExtension() + "", user.getPassword(), jData.toString());

	}

	public static String performAction(final User user, final String sData) {
		return performAction(user.getCompany(), user.getExtension() + "", user.getPassword(), sData);
	}

	private static String performAction(final String company, final String extension, final String password, final String input) {
		StringBuilder sResponse = new StringBuilder();

		String sData = input;

		// Remove the double backslashes added by JSONObject constructor
		sData = sData.replace("\\\\", "\\");

		if (Tools.getAPIversion() >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {
				}

				@Override
				public void checkServerTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = null;
			try {
				sc = SSLContext.getInstance("SSL");
			} catch (GeneralSecurityException e) {
				try {
					sc = SSLContext.getInstance("TLS");
				} catch (GeneralSecurityException e1) {
					Log.e("HTTPResources.performAction()", e1.getLocalizedMessage());
				}
			} finally {

				try {
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
				} catch (KeyManagementException e) {
					Log.e("HTTPResources.performAction()", e.getLocalizedMessage());
				}
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			}

			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(final String string, final SSLSession ssls) {
					return true;
				}
			});

			try {

				URL url = new URL(Constants.SERVER_URL);
				HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

				conn.setConnectTimeout(8000);
				conn.setDoOutput(true);
				conn.setRequestProperty("Authorization", "PLAINTEXT," + company + "," + extension + "," + password);

				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.writeBytes("data=" + sData);
				wr.flush();
				wr.close();

				InputStream stream = conn.getInputStream();

				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 32568);

				String sLine;

				while ((sLine = reader.readLine()) != null) {

					sResponse.append(sLine);
				}

				conn.disconnect();
			} catch (SocketTimeoutException e) {
				return null;
			} catch (IOException ex) {
				Log.e("HttpResources IOException", ex.getLocalizedMessage());
				return null;
			}
		} else {
			try {
				System.setProperty("http.keepAlive", "false");

				HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
				HttpPost request = new HttpPost(Constants.SERVER_URL);

				request.setHeader("Authorization", "PLAINTEXT," + company + "," + extension + "," + password);
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
				nameValuePairs.add(new BasicNameValuePair("data", sData));
				request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				SchemeRegistry registry = new SchemeRegistry();
				SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
				socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				registry.register(new Scheme("https", socketFactory, 443));

				HttpParams params = new BasicHttpParams();

				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

				SingleClientConnManager mngr = new SingleClientConnManager(params, registry);

				DefaultHttpClient httpClient = new DefaultHttpClient(mngr, params);

				HttpResponse response = httpClient.execute(request);

				String entityString = EntityUtils.toString(response.getEntity(), "UTF-8");
				sResponse.append(entityString);

			} catch (ClientProtocolException e) {
				Log.e("HTTPResources.performAction() ClientProtocolException", e.getLocalizedMessage());
			} catch (IOException e) {
				Log.e("HTTPResources.performAction() IOException", e.getLocalizedMessage());
			}
		}

		String responseString = sResponse.toString();
		Log.d(QUERY, sData);
		Log.d(RESPONSE, responseString);

		return sResponse.toString();
	}
}
