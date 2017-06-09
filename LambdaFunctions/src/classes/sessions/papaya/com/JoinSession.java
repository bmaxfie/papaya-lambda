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
			json = Validate.field(input, "body_json");
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
				String updateActive = "UPDATE users_sessions SET active=1 WHERE session_user_id='" + user_id + "' AND user_session_id='" + session_id + "'";
				statement.execute(updateActive);
			} else if(!pairExists) {
				String insertUserIntoSession = "INSERT INTO users_sessions VALUES ( 1, '" + user_id + "', '" + session_id + "' )";
				statement.execute(insertUserIntoSession);
			}
			statement.close();
			
			//brought from RemoveUserFromSessionUpdate Branch
			logger.log("Before getCurrentSessionID\n");
			//if current_session_id is set to something else, remove the user from that session and update their current_session_id to new
			String getCurrentSessionID = "SELECT current_session_id FROM users WHERE user_id='" + user_id + "'";
			statement = con.createStatement();
			result = statement.executeQuery(getCurrentSessionID);
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
			logger.log("Before checking cur_ses_id + " + curSessionIDExists + "\n");
			//if current_session_id exists, remove user from that session and transfer host if necessary: should not happen if everything works properly
			if(curSessionIDExists) {
				String removeUserFromSession = "UPDATE users_sessions SET active=0 "
						+ "WHERE session_user_id='" + user_id + "' AND user_session_id='" + result_session_id + "'";
				statement = con.createStatement();
				statement.execute(removeUserFromSession);
				statement.close();
				logger.log("after remove user, before isUserHost " + result_session_id +  "\n");
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

				logger.log("after IsUserHost before checkIfEmpty\n");
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
				logger.log("after checkIfEmpty, before transferHost");
				if(stillExists && userIsHost) {
					//change host to newHostIfNeeded
					String transferHost = "UPDATE sessions SET host_id='" + newHostIfNeeded + "' WHERE session_id='" + result_session_id + "'";
					statement = con.createStatement();
					statement.execute(transferHost);
					statement.close();
				}

			}
			logger.log("updating current session id to: " + session_id + "\n");
			//update current_session_id to match the new session_id
			String updateCurrentSessionID = "UPDATE users SET current_session_id='" + session_id + "' WHERE user_id='" + user_id + "'";
			statement = con.createStatement();
			statement.execute(updateCurrentSessionID);
			statement.close();
			//end brought code
			
			
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
