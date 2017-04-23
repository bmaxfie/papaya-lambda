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

public class InsertSession implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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
		String session_id, location_desc, description, class_id, start_time;
		String user_id = "";
		Integer duration;
		Double location_lat, location_long;
		
		int active = 1;
		// Optional request fields:
		Boolean sponsored;

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
			path = Validate.field(input, "params");
			path = Validate.field(path, "path");
			
			user_id = Validate.user_id(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			duration = Validate.duration(json);
			location_lat = Validate.location(json, "location_lat");
			location_long = Validate.location(json, "location_long");
			location_desc = Validate.description(json, "location_desc");
			description = Validate.description(json, "description");
			sponsored = Validate.sponsored(json);
			//start_time = Validate.start_time(json);
			class_id = Validate.class_id(path);
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
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		start_time = dateFormatter.format(new Date());

		/*
		 * ### Generate unique user_id number and validate its uniqueness.
		 */
		boolean exists = false;
		Connection con = getRemoteConnection(context);

		try {
			// Checks for necessary rows in tables:
			if (!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist in database.");
				return generate404("user_id not found in database.");
			}
			if (!classIDExists(class_id, con)) {
				logger.log("ERROR: 404 Not Found - class_id does not exist in database.");
				return generate404("class_id not found in database.");
			}

			session_id = UIDGenerator.generateUID(user_id);
			// Generates new UIDs:
			for (int i = 0; (exists = sessionIDExists(session_id, con)) && i < 3; i++) {
				//generate a session_id with user_id as the salt
				session_id = UIDGenerator.generateUID(user_id);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times. Try recalling.");
			}

			String insertSession = "INSERT INTO sessions VALUES ('" + session_id + "', '" 
						+ user_id + "', " + duration + ", '" + location_desc + "', '" 
						+ description + "', '" + sponsored.toString() + "', " + location_lat.floatValue() 
						+ ", " + location_long.floatValue() + ", '" + start_time + "')";
			Statement statement = con.createStatement();
			statement.execute(insertSession);
			statement.close();
			
			String insertClassSession = "INSERT INTO classes_sessions VALUES ('" + session_id + "', '" + class_id + "')";
			statement = con.createStatement();
			statement.execute(insertClassSession);
			statement.close();

			String insertUserSession = "INSERT INTO users_sessions VALUES (" + active + ", '" + user_id + "', '" + session_id + "')";
			statement = con.createStatement();
			statement.execute(insertUserSession);
			statement.close();
			
			//code added for host transfer
			//if current_session_id is set to something else, remove the user from that session and update their current_session_id to new
			String getCurrentSessionID = "SELECT current_session_id FROM users WHERE user_id='" + user_id + "'";
			statement = con.createStatement();
			ResultSet result = statement.executeQuery(getCurrentSessionID);
			boolean curSessionIDExists = false;
			String result_session_id = "";
			if(result.next()) {
				result_session_id = result.getString("current_session_id");
				if(!(result_session_id == null) && !result_session_id.equals("")) {
					curSessionIDExists = true;
				}
			}
			result.close();
			statement.close();
			
			//if current_session_id exists, remove user from that session and transfer host if necessary: should not happen if everything works properly
			if(curSessionIDExists) {
				String removeUserFromSession = "UPDATE users_sessions SET active=0 "
						+ "WHERE session_user_id='" + user_id + "' AND user_session_id='" + result_session_id + "'";
				statement = con.createStatement();
				statement.execute(removeUserFromSession);
				statement.close();

				//if the user is the study session host
				String isUserHost = "SELECT host_id FROM sessions WHERE session_id='" + result_session_id + "'";
				statement = con.createStatement();
				result = statement.executeQuery(isUserHost);
				boolean userIsHost = false;
				if(result.next()) {
					if(result.getString("host_id").equals(user_id)) {
						userIsHost = true;
					}
				}
				result.close();
				statement.close();

				String checkIfEmpty = "SELECT active, session_user_id FROM users_sessions WHERE user_session_id='" + result_session_id + "'";
				statement = con.createStatement();
				result = statement.executeQuery(checkIfEmpty);
				boolean stillExists = false;
				String newHostIfNeeded = "";
				while(result.next()) {
					if(result.getString("active") == "1") {
						String idResult = result.getString("session_user_id");
						if(!idResult.equals(user_id)) {
							newHostIfNeeded = idResult;
							stillExists = true;
						}
					}

				}

				result.close();
				statement.close();

				if(stillExists && userIsHost) {
					//change host to newHostIfNeeded
					String transferHost = "UPDATE sessions SET host_id='" + newHostIfNeeded + "' WHERE session_id='" + result_session_id + "'";
					statement = con.createStatement();
					statement.execute(transferHost);
					statement.close();
				}

			}

			//end of code added for host transfer

			String updateUser = "UPDATE users SET current_session_id='" + session_id + "' WHERE user_id='" + user_id + "'";
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
		response.put("code_description", "Created");
		response.put("session_id", session_id);
		response.put("class_id", class_id);
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
	
	private boolean classIDExists(String class_id, Connection dbcon) throws SQLException {
		String getClass = "SELECT class_id FROM classes WHERE class_id='"+class_id+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getClass);
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
