package website.papaya.com;

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

public class SessionsDump implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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
		ArrayList<Map<String, Object>> classes = new ArrayList<Map<String, Object>>();
		// Required request fields:
		String professor_access_key = "";
		//
		String class_id = "";
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist in string URL parameters:
		 * 			user_id
		 * 			authentication_key
		 * 			service
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		
		Map<String, Object> querystring;
		try {
			// Find Paths:
			querystring = Validate.field(input, "params");
			querystring = Validate.field(querystring, "querystring");
		
			// 3. validate 'authentication_key' is of length allowed?
			// TODO: Determine more strict intro rules
			professor_access_key = Validate.access_key(querystring);
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
			String getClassID = "SELECT class_id FROM classes WHERE c.professor_access_key='" + professor_access_key + "'";
			Statement classStatement = con.createStatement();
			ResultSet classResult = classStatement.executeQuery(getClassID);
			
			if (classResult.next()) {
				Map<String, Object> c = new HashMap<String, Object>();
				c.put("class_id", classResult.getString("class_id"));
				
				logger.log("Found class: " + classResult.getString("class_id") + "\n");
				class_id = classResult.getString("class_id");
				String getSessionInfo = "SELECT DISTINCT * "
						+ "FROM ( "
						+ "SELECT * "
						+ "FROM sessions AS s "
						+ "INNER JOIN ( "
						+ "SELECT DISTINCT user_session_id "
						+ "FROM users_sessions "
						+ ") sessionCheck ON s.session_id = sessionCheck.user_session_id "
						+ ") allSessions "
						+ "INNER JOIN ( "
						+ "SELECT DISTINCT class_session_id "
						+ "FROM classes_sessions "
						+ "WHERE session_class_id = '"+ class_id +"' "
						+ ") classFilter ON allSessions.session_id = classFilter.class_session_id;";
				Statement sessionStatement = con.createStatement();
				ResultSet sessionResult = sessionStatement.executeQuery(getSessionInfo);
				logger.log(getSessionInfo);
				ArrayList<Map<String, Object>> sessions = new ArrayList<Map<String, Object>>();
				while (sessionResult.next()) {
					logger.log("made it inside while loop");
					Map<String, Object> s = new HashMap<String, Object>();
					s.put("session_id", sessionResult.getString("session_id"));
					s.put("host_id", sessionResult.getString("host_id"));
					s.put("duration", sessionResult.getInt("duration"));
					s.put("location_desc", sessionResult.getString("location_desc"));
					s.put("description", sessionResult.getString("description"));
					s.put("sponsored", sessionResult.getBoolean("sponsor"));
					s.put("location_lat", sessionResult.getFloat("location_lat"));
					s.put("location_long", sessionResult.getFloat("location_long"));
					s.put("start_time", sessionResult.getString("start_time"));
					
					sessions.add(s);
				}
				c.put("sessions", sessions);
				classes.add(c);
				
				sessionResult.close();
				sessionStatement.close();
			}
			response.put("classes", classes);
			
			classResult.close();
			classStatement.close();

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
