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
import utils.papaya.com.UIDGenerator;
import utils.papaya.com.Authentication;

public class CreateUser implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
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
		
		// Check for required keys.
		if ( (!input.containsKey("username") 
						|| !(input.get("username") instanceof String)
						|| !((username = (String) input.get("username")) != null))
				|| (!input.containsKey("authentication_key") 
						|| !(input.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) input.get("authentication_key")) != null))
				|| (!input.containsKey("service") 
						|| !(input.get("service") instanceof String)
						|| !((service = (String) input.get("service")) != null)) ) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("username or authentication_key or service do not exist.", "");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		
		// 1. validate 'username' field is of length allowed in database, otherwise truncate.
		if (username.length() > 45)
			username = username.substring(0, 45);
		
		// 2. validate 'service' field is a recognizable type
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service + "'.");
			return throw400("service was of an unrecognizable type '" + service + "'.", "service");
		}
				
		// 2. validate 'authentication_key' is of length allowed?
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
		
		
		// 4. validate 'phone' is of acceptable length and format if it exists.
		if (input.containsKey("phone")
				&& (input.get("phone") instanceof Integer)) {
			
			// phone exists, now check if it is of proper format.
			phone = ((Integer) input.get("phone")).intValue();
			
			if (// phone is 7 digits
					!(phone > 999999l
						&& phone < 10000000l)
				&&
				// phone is 10 digits
					!(phone > 999999999l
						&& phone < 10000000000l)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. phone was not formatted right (i.e. neither 7 or 10 digits long).");
				return throw400("phone was not formatted right (i.e. neither 7 or 10 digits long) '" + phone + "'.", "phone");
			}
		}
		
		// 5. validate 'email' is of acceptable format and length if it exists.
		if (input.containsKey("email")
				&& (input.get("email") instanceof String)) {
			
			email = (String) input.get("email");
			
			if (email.length() > 45
					// Regex is supposed to loosely match general email form.
					|| !email.matches(".{3,}@.{3,}\\..{2,}")) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.");
				return throw400("email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.", "email");
			}
		}
		
		
		/*
		 * 2. Authentic authentication_key check:
		 * 
		 */
		// TODO: actually check authentication service here.
		
		
		/*
		 * 3a. Get any data from tables to complete request.
		 * 		
		 * 		1. Check if 
		 */
		
		/*
		 * ### Generate unique user_id number and validate its uniqueness.
		 */
		Connection con = getRemoteConnection(context);
		user_id = UIDGenerator.generateUID(username);
		
		for (int i = 0; userIDExists(user_id, con) && i < 3; i++) {
			user_id = UIDGenerator.generateUID(username);
		}
		
		try {

			String insertUser = "INSERT INTO users VALUES ('" + input.get("user_id") + "', '"
					+ input.get("username") + "', " + input.get("phone") + ", " + "'"
					+ input.get("email") + "', '" + input.get("authentication_key") + "', '"
					+ input.get("current_session_id") + "')";
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

			return throw500("SQL error.");
			
		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					return throw500("SQL error.");
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
    
    
    private boolean userIDExists(String user_id, Connection dbcon)
    {
    	try {
			String getUser = "SELECT user_id from users where user_id='"+user_id+"'";
			Statement statement = dbcon.createStatement();
			ResultSet result = statement.executeQuery(getUser);
			statement.close();
			if (result.next())
				return true;
			else
				return false;

		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

			return false;
			
		} finally {
			context.getLogger().log("Closing the connection.");
			if (dbcon != null)
				try {
					dbcon.close();
				} catch (SQLException ignore) {
					logger.log("SQL Error: Problem closing connection.");
				}
		}
    }
    
    
    /**
     * ERROR MESSAGE THROWING METHODS:
     */
    
    private static Map<String, Object> throw400(String message, String fields) {
    	Map<String, Object> response = new HashMap<String, Object>();
		response.put("code", 400);
		response.put("code_description", "Bad Request");
		response.put("error_description", message);
		response.put("fields", fields);
		return response;
    }
    
    private static Map<String, Object> throw500(String message) {
    	Map<String, Object> response = new HashMap<String, Object>();
    	response.put("code", 500);
    	response.put("code_description", "Internal Server Error");
    	response.put("error_description", message);
    	return response;
    }

}
