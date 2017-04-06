package classes.sessions.papaya.com;

import static utils.papaya.com.ResponseGenerator.*;

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
import utils.papaya.com.Authentication;
import utils.papaya.com.Exception400;
import utils.papaya.com.UIDGenerator;
import utils.papaya.com.Validate;

public class RemoveUser implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	/**
	 * Steps to implement a generic papaya API Lambda Function:
	 * 
	 * 1. Check request body (validate) for proper format of fields. 2. Ensure
	 * request is authentic from authentication service. 3a. Get any data from
	 * tables to complete request. 3b. Change tables as necessary for particular
	 * request.
	 * 
	 * For description of the API requirements, seek the API Documentation in
	 * developers folder.
	 */

	private Context context;
	private LambdaLogger logger;

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> json, path;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required fields:
		String authentication_key, service_user_id, service;
		String user_id = "";
		String current_session_id = ""; //does not need to be passed in through json, obtained from sql

		
		// Optional request fields:

		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 
		 * fields must exist: user_id authentication_key service
		 * 
		 * // TODO: Check for SQL INJECTION!
		 */
		
		try {
			// Find Paths:
			json = Validate.field(input, "body_json");
			
			user_id = Validate.user_id(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			
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
		 * 1. Check if
		 */

		/*
		 * ### Generate unique user_id number and validate its uniqueness.
		 */
		Connection con = getRemoteConnection(context);

		try {
			// Checks for necessary rows in tables:
			if (!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist in database.");
				return generate404("user_id not found in database.");
			}

			//gets the user's current session
			String getCurrentSession = "SELECT current_session_id FROM users WHERE user_id='" + user_id + "'";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getCurrentSession);
			if(result.next()) {
				current_session_id = result.getString("current_session_id");
			}
			result.close();
			statement.close();
			
			if(current_session_id.equals("")) {
				logger.log("ERROR: 404 Not Found - user is not currently in a session.");
				return generate404("user is not currently in a session.");
			}
			
			//TODO need to check if current_session_id matches a session_id in the sessions or user_sessions table?
			
			String removeUser = "UPDATE users_sessions SET active=0 WHERE session_user_id='" + user_id + "' AND user_session_id='" + current_session_id + "'";
			statement = con.createStatement();
			statement.execute(removeUser);
			statement.close();
		
//			//removes the user-session pair from the users-sessions table
//			String removeUser = "DELETE FROM users_sessions WHERE session_user_id='" + user_id + "' AND user_session_id='" + current_session_id + "'";
//			statement = con.createStatement();
//			statement.execute(removeUser);
//			statement.close();
//			
			//sets the current session of the user back to nothing as they are no longer in a session
			String updateUser = "UPDATE users SET current_session_id='' WHERE user_id='" + user_id + "'";
			statement = con.createStatement();
			statement.execute(updateUser);
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
		response.put("code_description", "Removed User from Session");
		response.put("current_session_id", current_session_id);
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
	
	private boolean userIDExists(String user_id, Connection dbcon) throws SQLException {
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

}
