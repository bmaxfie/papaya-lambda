package invitations.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate404;
import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Exception400;
import utils.papaya.com.Validate;

public class GetInvitations implements RequestHandler<Map<String, Object>, Map<String, Object>>{

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
		Map<String, Object> json, path;
		// Required request fields:
		String user_id = "", authentication_key = "", service_user_id = "";
		
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist in string path parameters:
		 * 			user_id
		 * 			service
		 * 			authentication_key
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		
		Map<String, Object> querystring;
		try {
			// Find Paths:
			querystring = Validate.field(input, "params");			
			path = Validate.field(querystring, "path");
			
			querystring = Validate.field(querystring, "querystring");
			

			
			// 1. validate 'user_id'
			user_id = Validate.user_id(querystring);
			// 2. validate 'service' field is a recognizable type
			service_type = Validate.service(querystring);				
			// 3. validate 'authentication_key' is of length allowed?
			// TODO: Determine more strict intro rules
			authentication_key = Validate.authentication_key(querystring, service_type);
			service_user_id = Validate.service_user_id(querystring, service_type);
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
			 * 3b. Change tables as necessary for particular request.
			 * 
			 * 		1. Update authentication_key for user_id.
			 */
						
			/*
			 * 
			 * SQL command: returns a list of friends for user: user_id
			 * 
			 */
			if (!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist in database.");
				return generate404("user_id not found in database.");
			}
			
			String getInvitationInfo = "SELECT session_class_id as class_id, invitation_session_id as session_id, username, classname FROM "
					+ "(SELECT session_class_id, invitation_session_id, username FROM "
					+ "(SELECT invitation_session_id, username FROM "
					+ "(SELECT sender_id, invitation_session_id, receiver_id FROM invitations WHERE receiver_id='" + user_id + "') invites "
					+ "INNER JOIN users AS ut ON ut.user_id=invites.sender_id) user_sess "
					+ "INNER JOIN classes_sessions AS cst ON cst.class_session_id=user_sess.invitation_session_id) class_info "
					+ "INNER JOIN classes as ct ON ct.class_id=class_info.session_class_id";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getInvitationInfo);
			
			ArrayList<Map<String, Object>> invites = new ArrayList<Map<String, Object>>();
			while (result.next()) {
				Map<String, Object> invite = new HashMap<String, Object>();
				invite.put("classname", result.getString("classname"));
				invite.put("class_id", result.getString("class_id"));
				invite.put("session_id", result.getString("session_id"));
				invite.put("username", result.getString("username"));
				invites.add(invite);
			}
			
			response.put("posts", invites.toArray());
			
			result.close();
			statement.close();
			
			String deleteInvitations = "DELETE FROM invitations WHERE receiver_id='" + user_id + "'";
			statement = con.createStatement();
			statement.execute(deleteInvitations);
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
}
