package users.papaya.com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Authentication;
import static utils.papaya.com.ResponseGenerator.*;

public class UpdateUserAuth implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		Integer auth_option;
		String user_id = "", username = "", email = "", authentication_key = "", service = "";
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			auth_option
		 * 				either:	user_id
		 * 				or:		username
		 * 						email
		 * 			authentication_key
		 * 			service
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		// Check for required keys.
		if ( (!papaya_json.containsKey("auth_option") 
						|| !(papaya_json.get("auth_option") instanceof Integer)
						|| !((auth_option = (Integer) papaya_json.get("auth_option")) != null))
				|| (!papaya_json.containsKey("authentication_key") 
						|| !(papaya_json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) papaya_json.get("authentication_key")) != null))
				|| (!papaya_json.containsKey("service"))
						|| !(papaya_json.get("service") instanceof String)
						|| !((service = (String) papaya_json.get("service")) != null)) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("auth_option or authentication_key or service do not exist.", "");
		}
		
		// Check for more required keys:
		// If 1, user_id is defined
		if (auth_option.intValue() == 1) {
			if (!papaya_json.containsKey("user_id")
						|| !(papaya_json.get("user_id") instanceof String)
						|| !((user_id = (String) papaya_json.get("user_id")) != null)) {
				logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
				return generate400("user_id does not exist.", "user_id");
			}
		} // If 2, username & email are defined
		else if (auth_option.intValue() == 2) {
			if (!papaya_json.containsKey("username")
						|| !(papaya_json.get("username") instanceof String)
						|| !((username = (String) papaya_json.get("username")) != null)
				|| !papaya_json.containsKey("email")
						|| !(papaya_json.get("email") instanceof String)
						|| !((username = (String) papaya_json.get("email")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required fields (username or email) did not exist or are empty.");
				return generate400("username or email does not exist.", "username, email");
			}
		}
		else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required auth_option to be of a set range of values.");
			return generate400("auth_option was not of a value in the range acceptable.", "auth_option");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		//		auth_option has already been verified.
		
		// 1. validate 'user_id' if auth_option == 1.
		if (auth_option.intValue() == 1) {
			if (!papaya_json.containsKey("user_id")
						|| !(papaya_json.get("user_id") instanceof String)
						|| !((user_id = (String) papaya_json.get("user_id")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required user_id doesn't exist despite given auth_option value.");
				return generate400("user_id does not exist.", "user_id");
				
			} else if (user_id.length() > 45) {
				user_id = user_id.substring(0, 45);
			}
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
		
		// 3. validate 'username' field if auth_option == 2. Check if field is of length allowed in database, otherwise truncate.
		if (auth_option.intValue() == 2) {
			if (!papaya_json.containsKey("username")
						|| !(papaya_json.get("username") instanceof String)
						|| !((username = (String) papaya_json.get("username")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required username doesn't exist despite given auth_option value.");
				return generate400("username does not exist.", "username");
				
			} else if (username.length() > 45) {
				username = username.substring(0, 45);
			}
		}
		
		// 4. validate 'email' field if auth_option == 2. Check if field is of acceptable format and length.
		if (auth_option.intValue() == 2 && papaya_json.containsKey("email")
				&& (papaya_json.get("email") instanceof String)) {
			
			email = (String) papaya_json.get("email");
			
			if (email.length() > 45
					// Regex is supposed to loosely match general email form.
					|| !email.matches(".{3,}@.{3,}\\..{2,}")) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.");
				return generate400("email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.", "email");
			}
		}
				
		// 5. validate 'authentication_key' is of length allowed?
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
			 * 3a. Get any data from tables to complete request.
			 * 
			 * 		1. Get user_id if not supplied from API call.
			 */
			if (auth_option.intValue() == 2) {
				String getuser_id = "SELECT user_id FROM users WHERE username='"+username+"' AND email='"+email+"'";
				Statement statement = con.createStatement();
				ResultSet result = statement.executeQuery(getuser_id);
				if (!result.next()) {
					result.close();
					statement.close();
					logger.log("ERROR: 404 Not Found - Returned to client. No user could be found with the username and email given.");
					return generate404("No user could be found with the username and email given.");
				}
				if (!result.isLast()) {
					result.close();
					statement.close();
					logger.log("ERROR: 500 Internal Server Error - Returned to client. More than one user was returned for username and email.");
					return generate500("More than one user was returned for username and client.");
				}
				
				user_id = result.getString(0);
				result.close();
				statement.close();
			}
			
			/*
			 * 3b. Change tables as necessary for particular request.
			 * 
			 * 		1. Update authentication_key for user_id.
			 */
			
			String setauth = "UPDATE users SET authentication_key='"+authentication_key+"' WHERE user_id='"+user_id+"'";
			Statement statement = con.createStatement();
			statement.executeUpdate(setauth);
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
