package posts.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate404;
import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Exception400;
import utils.papaya.com.UIDGenerator;
import utils.papaya.com.Validate;

public class CreatePost implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		String class_id = "";
		String post_id = ""; //created by the lambda function
		// Required request fields:
		String session_id, authentication_key, service_user_id, start_time, message;
		// Optional request fields:
		int visibility = 0;
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 		session_id, authentication_key, service_user_id
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		
		try {
			// Find Path:
			json = Validate.field(input, "body_json");
			
			// Validate required fields:
			session_id = Validate.session_id(json);
			class_id = Validate.class_id(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			start_time = Validate.start_time(json);
			message = Validate.message(json, "message");
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
		}
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		start_time = dateFormatter.format(new Date());
		
		try {
			visibility = Validate.visibility(json);
		} catch (Exception400 e400) {
			logger.log("visibility not found, but not required.\n");
			visibility = 0;
		}
		
		/*
		 * 2. Authentic authentication_key check:
		 * 
		 */
		// TODO: actually check authentication service here.
		
		
		/*
		 * 3a. Get any data from tables to complete request.
		 */
		
		/*
		 * ### Generate unique user_id number and validate its uniqueness.
		 */
		boolean exists = false;
		Connection con = getRemoteConnection(context);
		
		try {
			
			// Checks for necessary rows in tables: make sure user_id exists
			if (!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist in database.");
				return generate404("user_id not found in database.");
			}
			
			if(!sessionIDExists(session_id, con)) {
				logger.log("ERROR: 404 Not Found - session_id does not exist in database.");
				return generate404("session_id not found in database.");
			}
			
			if(!classIDExists(class_id, con)) {
				logger.log("ERROR: 404 Not Found - class_id does not exist in database.");
				return generate404("class_id not found in database.");
			}
			
			for (int i = 0; (exists = postIDExists(post_id, con)) && i < 3; i++) {
				post_id = UIDGenerator.generateUID(user_id + session_id);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID for post on 3 tries.");
				return generate500("generateUID() failed 3 times. Try recalling.");
			}
			
			int user_role = 0;
			String getUserRole = "SELECT user_role FROM users_classes WHERE class_user_id='" + user_id + "' AND user_class_id='" + class_id + ")";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getUserRole);
			if(result.next()) {
				result.getInt("user_role");
			}
			result.close();
			statement.close();
			
			String insertPost = "INSERT INTO posts VALUES ('" + post_id + "', '"
					+ session_id + "', '" + user_id + "', " + user_role + ", '" + start_time + "', '" + message + "', " + visibility + ")";
			statement = con.createStatement();
			statement.addBatch(insertPost);
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
		response.put("session_id", session_id);
		response.put("post_id", post_id);
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
    
    private boolean postIDExists(String post_id, Connection dbcon) throws SQLException
    {
		String getPost = "SELECT post_id FROM posts WHERE post_id='"+ post_id +"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getPost);
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
    
	private boolean sessionIDExists(String session_id, Connection dbcon) throws SQLException
    {
		String getSession = "SELECT session_id FROM sessions WHERE session_id='"+session_id+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getSession);
		if (result.next()) {
			result.close();
			statement.close();
			return true;
		} else {
			result.close();
			statement.close();
			return false; 
		}
    }
	
	private boolean classIDExists(String class_id, Connection dbcon) throws SQLException
    {
		String getClass = "SELECT class_id FROM classes WHERE class_id='"+class_id+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getClass);
		if (result.next()) {
			result.close();
			statement.close();
			return true;
		} else {
			result.close();
			statement.close();
			return false; 
		}
    }
}


