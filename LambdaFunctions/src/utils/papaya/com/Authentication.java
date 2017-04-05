package utils.papaya.com;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Authentication {
	
	public static final String SERVICE_FACEBOOK = "FACEBOOK";
	public static final String SERVICE_GOOGLE = "GOOGLE";
	
	public static final int FACEBOOK_KEY_MIN_LEN = 17;
	public static final int GOOGLE_KEY_MIN_LEN = 20;
	
	public static boolean isAuthorized(String authentication_key, String service_user_id, AuthServiceType service_type) throws GeneralSecurityException, IOException {
		if(service_type == AuthServiceType.GOOGLE) {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
				    .setAudience(Collections.singletonList(service_user_id)).build();
			if(verifier.verify(authentication_key) != null) {
				return true;
			}
		} else if(service_type == AuthServiceType.FACEBOOK) {
			
		}
		return false;
	}
}

