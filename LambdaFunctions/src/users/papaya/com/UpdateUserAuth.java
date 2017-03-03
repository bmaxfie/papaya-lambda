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
import utils.papaya.com.Exception400;
import utils.papaya.com.Validate;

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
		Map<String, Object> json;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		int auth_option;
		String user_id = "", username = "", email = "", authentication_key = "";
		
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
		
		try {
			// Find path:
			json = Validate.field(input, "body-json");
			
			// Validate required fields:
			auth_option = Validate.auth_option(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			if (auth_option == 1) {
				user_id = Validate.user_id(json);
			} else if (auth_option == 2) {
				username = Validate.username(json);
				email = Validate.email(json);
			} else {
				logger.log("ERROR: 400 Bad Request - Returned to client. Required auth_option to be of a set range of values.");
				return generate400("auth_option was not of a value in the range acceptable.", "auth_option");
			}
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
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
			if (auth_option == 2) {
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
