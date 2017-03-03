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
import utils.papaya.com.Validate;
import utils.papaya.com.Authentication;
import utils.papaya.com.Exception400;

import static utils.papaya.com.ResponseGenerator.*;

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
		Map<String, Object> json;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		String user_id = "";
		// Required request fields:
		String username, authentication_key;
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
		
		
		try {
			// Find Path:
			json = Validate.field(input, "body-json");
			
			// Validate required fields:
			username = Validate.username(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
		}
		// Try to validate optional fields:
		try {
			phone = Validate.phone(json);
		} catch (Exception400 e400) {
			logger.log("phone not found, but not required.");
		}
		try {
			email = Validate.email(json);
		} catch (Exception400 e400) {
			logger.log("email not found, but not required.");
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
		user_id = UIDGenerator.generateUID(username);
		boolean exists = false;
		Connection con = getRemoteConnection(context);
		
		try {
			
			for (int i = 0; (exists = userIDExists(user_id, con)) && i < 3; i++) {
				user_id = UIDGenerator.generateUID(username);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times. Try recalling.");
			}
			
			String insertUser = "INSERT INTO users VALUES ('" + user_id + "', '"
					+ username + "', " + phone + ", " + "'"
					+ email + "', '" + authentication_key + "', '" + "')";
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
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
					return generate500("SQL error.");
				}
		}
		
		response.put("code", 201);
		response.put("code_description", "Created");
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
    
    
    private boolean userIDExists(String user_id, Connection dbcon) throws SQLException
    {
		String getUser = "SELECT user_id FROM users WHERE user_id='"+user_id+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getUser);
		if (result.next()) {
			result.close();
			statement.close();
			return true;
		}
		else {
			result.close();
			statement.close();
			return false;
		}
    }
    
}
