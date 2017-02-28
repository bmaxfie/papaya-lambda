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
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		Integer auth_option;
		String user_id, username, email, authentication_key;
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			auth_option
		 * 				either:	user_id
		 * 				or:		username
		 * 						email
		 * 			authentication_key
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		// Check for required keys.
		if ( (!input.containsKey("auth_option") 
						|| !(input.get("auth_option") instanceof Integer)
						|| !((auth_option = (Integer) input.get("auth_option")) != null))
				|| (!input.containsKey("authentication_key") 
						|| !(input.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) input.get("authentication_key")) != null))) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("username or authentication_key or service do not exist.", "");
		}
		
		// Check for more required keys:
		// If 1, user_id is defined
		if (auth_option.intValue() == 1) {
			if (!input.containsKey("user_id")
						|| !(input.get("user_id") instanceof String)
						|| !((user_id = (String) input.get("user_id")) != null)) {
				logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
				return throw400("user_id does not exist.", "user_id");
			}
		} // If 2, username & email are defined
		else if (auth_option.intValue() == 2) {
			if (!input.containsKey("username")
						|| !(input.get("username") instanceof String)
						|| !((username = (String) input.get("username")) != null)
				|| !input.containsKey("email")
						|| !(input.get("email") instanceof String)
						|| !((username = (String) input.get("email")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required fields (username or email) did not exist or are empty.");
				return throw400("username or email does not exist.", "username, email");
			}
		}
		else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required auth_option to be of a set range of values.");
			return throw400("auth_option was not of a value in the range acceptable.", "auth_option");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		//		auth_option has already been verified.
		
		// 1. validate 'user_id' if auth_option == 1.
		if (auth_option.intValue() == 1) {
			if (!input.containsKey("user_id")
						|| !(input.get("user_id") instanceof String)
						|| !((user_id = (String) input.get("user_id")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required user_id doesn't exist despite given auth_option value.");
				return throw400("user_id does not exist.", "user_id");
				
			} else if (user_id.length() > 45) {
				user_id = user_id.substring(0, 45);
			}
		}
		
		// 2. validate 'username' field if auth_option == 2. Check if field is of length allowed in database, otherwise truncate.
		if (auth_option.intValue() == 2) {
			if (!input.containsKey("username")
						|| !(input.get("username") instanceof String)
						|| !((username = (String) input.get("username")) != null)) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. Required username doesn't exist despite given auth_option value.");
				return throw400("username does not exist.", "username");
				
			} else if (username.length() > 45) {
				username = username.substring(0, 45);
			}
		}
		
		// 3. validate 'email' field if auth_option == 2. Check if field is of acceptable format and length.
		if (auth_option.intValue() == 2 && input.containsKey("email")
				&& (input.get("email") instanceof String)) {
			
			email = (String) input.get("email");
			
			if (email.length() > 45
					// Regex is supposed to loosely match general email form.
					|| !email.matches(".{3,}@.{3,}\\..{2,}")) {
				
				logger.log("ERROR: 400 Bad Request - Returned to client. email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.");
				return throw400("email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.", "email");
			}
		}
				
		// 4. validate 'authentication_key' is of length allowed?
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
		Connection con = getRemoteConnection(context);
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
