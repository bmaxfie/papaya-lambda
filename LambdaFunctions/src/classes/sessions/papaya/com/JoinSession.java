package classes.sessions.papaya.com;

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
import utils.papaya.com.Exception400;
import utils.papaya.com.UserRole;
import utils.papaya.com.Validate;

import static utils.papaya.com.ResponseGenerator.*;

public class JoinSession implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		Map<String, Object> json, path;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		String user_id, authentication_key, service_user_id, service, session_id; //class_id, session_id passed in through path
		// Optional request fields:
			//none
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 			access_key
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		try {
			// Find paths:
			json = Validate.field(input, "body-json");
			path = Validate.field(input, "params");
			path = Validate.field(path, "path");
			
			// 1. validate 'user_id' field is of length allowed in database, otherwise truncate.
			user_id = Validate.user_id(json);
			// 2. validate 'service' and check if it matches defined service types. 
			service_type = Validate.service(json);
			// 3. validate 'authentication_key' is of length allowed?
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			session_id = Validate.session_id(path);
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
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
			Statement statement = con.createStatement();
			//if user_id, session_id pair exists in users_sessions table, update active if active is not 0
			//if user_id, session_id pair does not exist in table, insert them with active equal to 1
			String getActive = "SELECT active FROM users_sessions WHERE session_user_id='" + user_id + "' AND user_session_id='" + session_id + "'";
			ResultSet result = statement.executeQuery(getActive);
			int active = 0;
			boolean pairExists = false;
			if(result.next()) {
				active = result.getInt("active");
				pairExists = true;
			}
			result.close();
			statement.close();
			
			statement = con.createStatement();
			if(pairExists && active == 0) {
				String updateActive = "UPDATE users_sessions SET active=1 WHERE session_user_id='" + user_id + "' AND user_session_id='" + session_id + "'";;
				statement.execute(updateActive);
			} else if(!pairExists) {
				String insertUserIntoSession = "INSERT INTO users_sessions VALUES ( 1, '" + user_id + "', '" + session_id + "' )";
				statement.execute(insertUserIntoSession);
			}
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
    
}
