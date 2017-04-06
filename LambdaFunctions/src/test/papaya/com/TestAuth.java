package test.papaya.com;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;


import utils.papaya.com.AuthServiceType;

public class TestAuth {
	public static void main(String[] args) {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
				JacksonFactory.getDefaultInstance()).setAudience(Collections.singletonList("107521950483367654580"))
						.build();

		try {
			GoogleIdToken idToken = verifier.verify(
					"eyJhbGciOiJSUzI1NiIsImtpZCI6ImY3ZjBhMzgxMjUwOTQ0"
					+ "ZWFkMjRlM2EyNDg1MzVlNGE4MDg4OWVhNTIifQ.eyJhenAiO"
					+ "iI3Mzk3MjAyOTM4ODMtbWUycm1hdWVmNDcydXM1NDRkYmw3O"
					+ "WJsMGJzcmxrajAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb"
					+ "20iLCJhdWQiOiI3Mzk3MjAyOTM4ODMtdmhvc2dsaXJ0bGw4NG"
					+ "I4Y3NnZXVhaWtnZjZiN2cyMGIuYXBwcy5nb29nbGV1c2VyY29ud"
					+ "GVudC5jb20iLCJzdWIiOiIxMDc1MjE5NTA0ODMzNjc2NTQ1ODAiLC"
					+ "JlbWFpbCI6ImJyYXZlZ3V5OTZAZ21haWwuY29tIiwiZW1haWxfdmVyaW"
					+ "ZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbS"
					+ "IsImlhdCI6MTQ5MTQ2MjU0NCwiZXhwIjoxNDkxNDY2MTQ0LCJuYW1lIjoiQWRh"
					+ "bSBKb2huc3RvbiIsInBpY3R1cmUiOiJodHRwczovL2xoNS5nb29nbGV1c2VyY29ud"
					+ "GVudC5jb20vLW8xbUxmTEdQTmNBL0FBQUFBQUFBQUFJL0FBQUFBQUFBQUFBL0FNY"
					+ "0FZaV9JZzFMQngtejRfU1o5VzJVcTROTVpzY2JEdFEvczk2LWMvcGhvdG8uanBnIi"
					+ "wiZ2l2ZW5fbmFtZSI6IkFkYW0iLCJmYW1pbHlfbmFtZSI6IkpvaG5zdG9uIiwibG9"
					+ "jYWxlIjoiZW4ifQ.WbiYRtmZkYHEFfjc-1rhlrnrdWChzdqndyqVspPpBCNK1DS_l"
					+ "2SUThKX4YbOgilWjH5VVMmXyqmZwDBf-ATunVHU-nMO_XXTMvAxC0kZqQYcGjCkBL"
					+ "PRehFV815ZzKBlBVYKyb39ILY3MayxPlwcGX_0KKExi1SX33CLDxe2TcUgmS_o109"
					+ "13snrIhihmQHBVgcpYJlA-u344zOYWxyrAQVUkupEeTIUkm7rh_rgg3sdN9x0wQYc"
					+ "C3Mac6XyH5e-hyNvuC6SOnWZHCOSuk37dKLPnVhKPRPVWzcTadRQKEyoeueVcEyKG"
					+ "XetdyK-Av1GvCgX-ba-QB3IQ0-2nqg3Hg");
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
