package classes.sessions.papaya.com;

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

public class RetrieveClassSessions implements RequestHandler <Map<String, Object>, Map<String, Object>>{

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
		String user_id = "", authentication_key = "", service_user_id = "", service = "", class_id = "";
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist in querystring:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 		
		 * 		field from path, class_id from class/{id}/sessions
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		Map<String, Object> path;
		Map<String, Object> querystrings;
		
		try {
			// Find Paths:
			path = Validate.field(input, "params");
			querystrings = Validate.field(path, "querystring");
			path = Validate.field(path, "path");
			
			// 1. validate 'user_id'
			user_id = Validate.user_id(querystrings);
			// 2. validate 'service' field is a recognizable type
			service_type = Validate.service(querystrings);
			// 3. validate 'authentication_key' is of length allowed?
			authentication_key = Validate.authentication_key(querystrings, service_type);
			service_user_id = Validate.service_user_id(querystrings, service_type);
			// 4. validate the path parameter 'class_id'
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
		
		
		
		Connection con = getRemoteConnection(context);
		try {
			
			/*
			 * 3b. Change tables as necessary for particular request.
			 * 
			 * 		1. Update authentication_key for user_id.
			 */
			
			// TODO: Accidentally made class retriever instead of session retriever.
			String setauth = "SELECT * FROM sessions AS s, classes_sessions AS c_s, users_sessions AS u_s WHERE c_s.session_class_id='"+class_id+"' AND u_s.active=1 AND s.session_id=c_s.class_session_id";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(setauth);
			
			ArrayList<Map<String, Object>> sessions = new ArrayList<Map<String, Object>>();
			while (result.next()) {
				Map<String, Object> session = new HashMap<String, Object>();
				session.put("session_id", result.getString("session_id"));
				session.put("host_id", result.getString("host_id"));
				session.put("duration", result.getInt("duration"));
				session.put("location_desc", result.getString("location_desc"));
				session.put("description", result.getString("description"));
				session.put("sponsored", result.getBoolean("sponsor"));
				session.put("location_lat", result.getFloat("location_lat"));
				session.put("location_long", result.getFloat("location_long"));
				session.put("start_time", result.getString("start_time"));
				sessions.add(session);
			}
			response.put("sessions", sessions);
			
			result.close();
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
    	
}
