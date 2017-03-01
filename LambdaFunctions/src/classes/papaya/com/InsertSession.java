package classes.papaya.com;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import users.papaya.com.CreateUser.SERVICE_TYPE;

public class InsertSession implements RequestHandler<Map<String, Object>, String> {
	/** Steps to implement a generic papaya API Lambda Function:
	 * 
	 * 	1.	Check request body (validate) for proper format of fields.
	 * 	2.	Ensure request is authentic from authentication service.
	 * 	3a.	Get any data from tables to complete request.
	 * 	3b.	Change tables as necessary for particular request.
	 * 
	 * 	For description of the API requirements, seek the API Documentation in developers folder.
	 */
	
	private static final String SERVICE_FACEBOOK = "FACEBOOK";
	private static final String SERVICE_GOOGLE = "GOOGLE";
	
	private static final int FACEBOOK_KEY_MAX_LEN = 45;
	private static final int FACEBOOK_KEY_MIN_LEN = 40;
	private static final int GOOGLE_KEY_MAX_LEN = 45;
	private static final int GOOGLE_KEY_MIN_LEN = 40;
	
	private static enum SERVICE_TYPE {
		FACEBOOK, GOOGLE, NONE
	}
	
	private Context context;
	private LambdaLogger logger;
	
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		
		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> response = new HashMap<String, Object>();
		SERVICE_TYPE service_type = SERVICE_TYPE.NONE;
		String user_id = "";
		// Required request fields:
		String username, authentication_key, service;
		// Optional request fields:
		long phone = 0;
		String email = null;
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			username
		 * 			phone (optional)
		 * 			email (optional)
		 * 			authentication_key
		 * 			service
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		Connection con = getRemoteConnection(context);
	}

}
