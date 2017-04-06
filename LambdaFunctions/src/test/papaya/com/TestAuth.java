package test.papaya.com;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;


import utils.papaya.com.AuthServiceType;

public class TestAuth {
	private final static String CLIENT_ID = "351771521851-8u2ppc3cvb4p2c8jj5k9um7nhg995d0r.apps.googleusercontent.com";
	private final static String TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY3ZjBhMzgxMjUwOTQ0ZWFkMjRlM2EyNDg1MzVlNGE4MDg4OWVhNTIifQ.eyJhenAiOiI3Mzk3MjAyOTM4ODMtbWUycm1hdWVmNDcydXM1NDRkYmw3OWJsMGJzcmxrajAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI3Mzk3MjAyOTM4ODMtdmhvc2dsaXJ0bGw4NGI4Y3NnZXVhaWtnZjZiN2cyMGIuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDc1MjE5NTA0ODMzNjc2NTQ1ODAiLCJlbWFpbCI6ImJyYXZlZ3V5OTZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbSIsImlhdCI6MTQ5MTQ2MjU0NCwiZXhwIjoxNDkxNDY2MTQ0LCJuYW1lIjoiQWRhbSBKb2huc3RvbiIsInBpY3R1cmUiOiJodHRwczovL2xoNS5nb29nbGV1c2VyY29udGVudC5jb20vLW8xbUxmTEdQTmNBL0FBQUFBQUFBQUFJL0FBQUFBQUFBQUFBL0FNY0FZaV9JZzFMQngtejRfU1o5VzJVcTROTVpzY2JEdFEvczk2LWMvcGhvdG8uanBnIiwiZ2l2ZW5fbmFtZSI6IkFkYW0iLCJmYW1pbHlfbmFtZSI6IkpvaG5zdG9uIiwibG9jYWxlIjoiZW4ifQ.WbiYRtmZkYHEFfjc-1rhlrnrdWChzdqndyqVspPpBCNK1DS_l2SUThKX4YbOgilWjH5VVMmXyqmZwDBf-ATunVHU-nMO_XXTMvAxC0kZqQYcGjCkBLPRehFV815ZzKBlBVYKyb39ILY3MayxPlwcGX_0KKExi1SX33CLDxe2TcUgmS_o10913snrIhihmQHBVgcpYJlA-u344zOYWxyrAQVUkupEeTIUkm7rh_rgg3sdN9x0wQYcC3Mac6XyH5e-hyNvuC6SOnWZHCOSuk37dKLPnVhKPRPVWzcTadRQKEyoeueVcEyKGXetdyK-Av1GvCgX-ba-QB3IQ0-2nqg3Hg";
	public static void main(String[] args) {
		
		
		try {
			HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport,
					JacksonFactory.getDefaultInstance()).setAudience(Collections.singletonList(CLIENT_ID))
							.build();
			
			System.out.println(TOKEN);
			GoogleIdToken idToken = verifier.verify(TOKEN);
			if (idToken != null) {
				System.out.println("Yes!");
			} else {
				System.out.println("No!");
			}
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done");
	}

}
