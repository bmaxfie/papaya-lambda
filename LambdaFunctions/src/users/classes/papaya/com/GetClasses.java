package users.classes.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate400;
import static utils.papaya.com.ResponseGenerator.generate500;

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

public class GetClasses implements RequestHandler<Map<String, Object>, Map<String, Object>>{

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
		Map<String, Object> papaya_json;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		String user_id = "", authentication_key = "", service = "", class_id = "";
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist in string URL parameters:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		// Get path to querystrings
		if (!input.containsKey("params")
						|| !(input.get("params") instanceof Map)
						|| !((papaya_json = (Map<String, Object>) input.get("params")) != null)
				|| !papaya_json.containsKey("querystring")
						|| !(papaya_json.get("querystring") instanceof Map)
						|| !((papaya_json = (Map<String, Object>) papaya_json.get("querystring")) != null)) {
			
			logger.log("ERROR: 400 Bad Request - Returned to client. Required path to querystring did not exist or are empty.");
			return generate400("path to querystring does not exist.", "params or querystring");
		}
		
		// Check for required keys.
		if (!papaya_json.containsKey("user_id")
						|| !(papaya_json.get("user_id") instanceof String)
						|| !((user_id = (String) papaya_json.get("user_id")) != null)
				|| (!papaya_json.containsKey("authentication_key") 
						|| !(papaya_json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) papaya_json.get("authentication_key")) != null))
				|| (!papaya_json.containsKey("service"))
						|| !(papaya_json.get("service") instanceof String)
						|| !((service = (String) papaya_json.get("service")) != null)) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("user_id or authentication_key or service do not exist.", "");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		//		auth_option has already been verified.
		
		// 1. validate 'user_id'
		if (user_id.length() > 45) {
			user_id = user_id.substring(0, 45);
		}
		
		// 2. validate 'service' field is a recognizable type
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service + "'.");
			return generate400("service was of an unrecognizable type '" + service + "'.", "service");
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
			return generate400("authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", "authentication_key");
		}
		
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
			String getclasses = "SELECT user_class_id FROM users_classes WHERE class_user_id='"+user_id+"'";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getclasses);
			
			ArrayList<String> class_ids = new ArrayList<String>();
			while (result.next()) {
				class_ids.add(result.getString(1));
			}
			response.put("class_ids", class_ids.toArray());
			
			result.close();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

			return generate500(ex.getMessage());
			
		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					return generate500(ignore.getMessage());
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
