package utils.papaya.com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Authentication {
	
	public static final String SERVICE_FACEBOOK = "FACEBOOK";
	public static final String SERVICE_GOOGLE = "GOOGLE";
	
	public static final int FACEBOOK_KEY_MIN_LEN = 17;
	public static final int GOOGLE_KEY_MIN_LEN = 20;
	
	public static boolean isAuthorized(String authentication_key, String service_user_id, AuthServiceType service_type) throws GeneralSecurityException, IOException, JSONException {
		if(service_type == AuthServiceType.GOOGLE) {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
				    .setAudience(Collections.singletonList(service_user_id)).build();
			if(verifier.verify(authentication_key) != null) {
				return true;
			}
		} else if(service_type == AuthServiceType.FACEBOOK) {

			String url_name = "https://graph.facebook.com/" + service_user_id + "/access_token=" + authentication_key;
			
			try {
				StringBuilder result = new StringBuilder();
				URL url = new URL(url_name);
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String line;
				while((line = rd.readLine()) != null) {
					result.append(line);
				}
				rd.close();
				JSONObject fb_return = new JSONObject(result);
				fb_return.getString("access_token");

	    } catch (MalformedURLException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		}
		return false;
	}
}

