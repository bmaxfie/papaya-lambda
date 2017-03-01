package users.classes.papaya.com;

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
import utils.papaya.com.UserRole;
import static utils.papaya.com.ResponseGenerator.*;

public class JoinClass implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		String user_id, authentication_key, service, access_key;
		// Optional request fields:
			//none
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		// Check for required keys.
		if ( (!input.containsKey("user_id") 
						|| !(input.get("user_id") instanceof String)
						|| !((user_id = (String) input.get("user_id")) != null))
				|| (!input.containsKey("authentication_key") 
						|| !(input.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) input.get("authentication_key")) != null))
				|| (!input.containsKey("service") 
						|| !(input.get("service") instanceof String)
						|| !((service = (String) input.get("service")) != null)) 
				|| (!input.containsKey("access_key") 
						|| !(input.get("access_key") instanceof String)
						|| !((access_key = (String) input.get("access_key")) != null)) 
			) {
			
			// TODO: Add "fields" that were actually the problem.
	    	logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("username or authentication_key or service do not exist.", "");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		
		// 1. validate 'username' field is of length allowed in database, otherwise truncate.
		if (user_id.length() > 45)
			user_id = user_id.substring(0, 45);
		
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
		boolean exists = false;
		Connection con = getRemoteConnection(context);
		
		try {
			Statement statement = con.createStatement();
			
			//get the class_id
			String get_class_id = "SELECT class_id FROM classes WHERE student_access_key='" + access_key + "' OR ta_access_key='" + access_key + "' OR professor_access_key='" + access_key +"'";
			ResultSet class_id_rs = statement.executeQuery(get_class_id);
			String class_id = "";
			if (class_id_rs.next()) {
				 class_id = class_id_rs.getString("class_id");
			}
			else {
				logger.log("ERROR: 400 Bad Request - Returned to client. class_id does not exist or is invalid.");
				return throw400("class_id does not exist or is invalid", "class_id");
			}
			
			//get the user_role
			int user_role = 0;
			String getAuthKey = "SELECT student_access_key, ta_access_key, professor_access_key FROM classes WHERE class_id=" + class_id;
			ResultSet getAuthKeyRS = statement.executeQuery(getAuthKey);
			if (getAuthKeyRS.next()) {
				String student = getAuthKeyRS.getString("student_access_key");
				String ta = getAuthKeyRS.getString("ta_access_key");
				String prof = getAuthKeyRS.getString("professor_access_key");
				if (student.equals(access_key))
					user_role = UserRole.value(UserRole.STUDENT);
				else if (ta.equals(access_key))
					user_role = UserRole.value(UserRole.TA);
				else if (prof.equals(access_key))
					user_role = UserRole.value(UserRole.PROFESSOR);
				else {
					logger.log("ERROR: 400 Bad Request - Returned to client. user_role is not a value [1-3].");
					return throw400("user_role is not a value [1-3].", "user_role");
				}
			}
			
			
			String joinClass = "INSERT INTO users_classes VALUES (" + user_role + ", '"
					+ user_id + "', '" + class_id + "')";
			
			statement.executeQuery(joinClass);
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
		statement.close();
		if (result.next())
			return true;
		else
			return false;
    }
    
}
