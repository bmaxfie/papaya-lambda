package classes.sessions.papaya.com;

import static utils.papaya.com.ResponseGenerator.throw400;
import static utils.papaya.com.ResponseGenerator.throw404;
import static utils.papaya.com.ResponseGenerator.throw500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Authentication;

public class RetrieveClassSessions implements RequestHandler <Map<String, Object>, Map<String, Object>>{

	/** Steps to implement a generic papaya API Lambda Function:
	 * 
	 * 	1.	Check request body (validate) for proper format of fields.
	 * 	2.	Ensure request is authentic from authentication service.
	 * 	3a.	Get any data from tables to complete request.
	 * 	3b.	Change tables as necessary for particular request.
	 * 
	 * 	For description of the API requirements, seek the API Documentation in developers folder.
	 */
	

	private Context context;
	private LambdaLogger logger;
	
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		
		this.context = context;
		this.logger = context.getLogger();
		@SuppressWarnings("unchecked")
		Map<String, Object> papaya_json = (Map<String, Object>) input.get("body-json");
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		String user_id = "", authentication_key = "", service = "", class_id = "";
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 		
		 * 		field from path, class_id from class/{id}/sessions
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		// Check for required keys.
		if ((!papaya_json.containsKey("authentication_key") 
						|| !(papaya_json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) papaya_json.get("authentication_key")) != null))
				|| (!papaya_json.containsKey("service"))
						|| !(papaya_json.get("service") instanceof String)
						|| !((service = (String) papaya_json.get("service")) != null)) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("username or authentication_key or service do not exist.", "");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		//		auth_option has already been verified.
		
		// 1. validate 'user_id'
		if (!papaya_json.containsKey("user_id")
					|| !(papaya_json.get("user_id") instanceof String)
					|| !((user_id = (String) papaya_json.get("user_id")) != null)) {
			
			logger.log("ERROR: 400 Bad Request - Returned to client. Required user_id doesn't exist despite given auth_option value.");
			return throw400("user_id does not exist.", "user_id");
			
		} else if (user_id.length() > 45) {
			user_id = user_id.substring(0, 45);
		}
		
		// 2. validate 'service' field is a recognizable type
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service + "'.");
			return throw400("service was of an unrecognizable type '" + service + "'.", "service");
		}
				
		// 3. validate 'authentication_key' is of length allowed?
		// TODO: Determine more strict intro rules
		if (service_type == AuthServiceType.FACEBOOK 
						&& (authentication_key.length() > Authentication.FACEBOOK_KEY_MAX_LEN
								|| authentication_key.length() < Authentication.FACEBOOK_KEY_MIN_LEN)
				|| service_type == AuthServiceType.GOOGLE
						&& (authentication_key.length() > Authentication.GOOGLE_KEY_MAX_LEN
								|| authentication_key.length() < Authentication.GOOGLE_KEY_MIN_LEN)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.");
			return throw400("authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", "authentication_key");
		}
		
		// 4. validate the path parameter 'class_id'
		
		
		
		/*
		 * 2. Authentic authentication_key check:
		 * 
		 */
		// TODO: actually check authentication service here.
		
		
		
		Connection con = getRemoteConnection(context);
		try {
			
			/*
			 * 3b. Change tables as necessary for particular request.
			 * 
			 * 		1. Update authentication_key for user_id.
			 */
			
			// TODO: Accidentally made class retriever instead of session retriever.
			String setauth = "SELECT class_session_id FROM classes_sessions WHERE session_class_id='"+class_id+"' AND active="+1+"";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(setauth);
			
			ArrayList<String> class_ids = new ArrayList<String>();
			while (result.next()) {
				class_ids.add(result.getString(0));
			}
			response.put("class_ids", class_ids);
			
			result.close();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

			return throw500(ex.getMessage());
			
		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					return throw500(ignore.getMessage());
				}
		}
		
		response.put("code", 200);
		response.put("code_description", "OK");
		response.put("user_id", user_id);
		response.put("authentication_key", authentication_key);
		return response;
	}
    
    public static Connection getRemoteConnection(Context context) {
		try {
			Class.forName(System.getenv("JDBC_DRIVER"));
			String dbName = System.getenv("RDS_DB_NAME");
			String userName = System.getenv("RDS_USERNAME");
			String password = System.getenv("RDS_PASSWORD");
			String hostname = System.getenv("RDS_HOSTNAME");
			String port = System.getenv("RDS_PORT");
			String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password="
					+ password;
			context.getLogger().log("Before Connection Attempt \n" + jdbcUrl + "\n");
			Connection con = DriverManager.getConnection(jdbcUrl);
			if (con == null) {
				context.getLogger().log("Connection is null");
			}
			context.getLogger().log("Success");
			return con;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
    	
}
