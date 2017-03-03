package classes.sessions.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate400;
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
import utils.papaya.com.Authentication;
import utils.papaya.com.UIDGenerator;

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
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required fields:
		String authentication_key, service;
		String session_id, location_desc, description, class_id;
		String user_id = "";
		Integer duration;
		Double location_lat, location_long;
		//TODO account for timezone
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startTime = sdf.format(date);
		int active = 1;
		// Optional request fields:
		String sponsor = "";

		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 
		 * fields must exist: user_id authentication_key service
		 * 
		 * // TODO: Check for SQL INJECTION!
		 */

		// Check for required key
		if ((!papaya_json.containsKey("user_id") 
						|| !(papaya_json.get("user_id") instanceof String)
						|| !((user_id = (String) papaya_json.get("user_id")) != null))
				|| (!papaya_json.containsKey("authentication_key") 
						|| !(papaya_json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) papaya_json.get("authentication_key")) != null))
				|| (!papaya_json.containsKey("service") 
						|| !(papaya_json.get("service") instanceof String)
						|| !((service = (String) papaya_json.get("service")) != null))) {

			// TODO: Add "fields" that were actually the problem.
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("user_id or authentication_key or service do not exist.", "");
		}
		if (!papaya_json.containsKey("duration") 
				|| !(papaya_json.get("duration") instanceof Integer)
				|| !((duration = (Integer) papaya_json.get("duration")) != null)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("Duration does not exist", "");
		}
		if (!papaya_json.containsKey("location_lat") 
				|| !(papaya_json.get("location_lat") instanceof Double)
				|| !((location_lat = (Double) papaya_json.get("location_lat")) != null)) {
			logger.log("loc_lat: " + papaya_json.get("location_lat").getClass().getName());
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("location_lat does not exist", "");
		}
		if (!papaya_json.containsKey("location_long") 
				|| !(papaya_json.get("location_long") instanceof Double)
				|| !((location_long = (Double) papaya_json.get("location_long")) != null)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("location_long does not exist.", "");
		}
		if ((!papaya_json.containsKey("location_desc") 
				|| !(papaya_json.get("location_desc") instanceof String)
				|| !((location_desc = (String) papaya_json.get("location_desc")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("location_desc does not exist.", "");
		}
		
		if ((!papaya_json.containsKey("description") 
				|| !(papaya_json.get("description") instanceof String)
				|| !((description = (String) papaya_json.get("description")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return generate400("description does not exist.", "");
		}

		Map<String, Object> path;
		if(!input.containsKey("params")
				|| !(input.get("params") instanceof Map)
				|| !((path = (Map<String, Object>) input.get("params")) != null)) {
			logger.log("Could not access params field of AWS transformed JSON.");
			return generate400("params field, when looking for the class_id is not in AWS transformed JSON.", "class_id");
		}
		if(!(path.containsKey("path"))
				|| !(path.get("path") instanceof Map)
				|| !((path = (Map<String, Object>) path.get("path")) != null)) {
			logger.log("Could not access path field of AWS ransformed JSON.");
			return generate400("path field, when looking for the class_id is not in AWS transformed JSON.", "class_id");
		}
		if(!(path.containsKey("class-id")) 
				|| !(path.get("class-id") instanceof String)
				|| !((class_id = (String) path.get("class-id")) != null)) {
			logger.log("id does not exist/is null");
			return generate400("id does not exist/is null.", "");
		}
		
		// 2. validate 'service' field is a recognizable type
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service
					+ "'.");
			return generate400("service was of an unrecognizable type '" + service + "'.", "service");
		}

		// 2. validate 'authentication_key' is of length allowed?
		// TODO: Determine more strict intro rules
		if (service_type == AuthServiceType.FACEBOOK
				&& (authentication_key.length() > Authentication.FACEBOOK_KEY_MAX_LEN
						|| authentication_key.length() < Authentication.FACEBOOK_KEY_MIN_LEN)
				|| service_type == AuthServiceType.GOOGLE
						&& (authentication_key.length() > Authentication.GOOGLE_KEY_MAX_LEN
								|| authentication_key.length() < Authentication.GOOGLE_KEY_MIN_LEN)) {
			logger.log(
					"ERROR: 400 Bad Request - Returned to client. authentication_key was not of valid length, instead it was '"
							+ authentication_key.length() + "'.");
			return generate400(
					"authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.",
					"authentication_key");
		}


		// 5. validate 'sponsor' is of acceptable format and length if it exists.
		if (papaya_json.containsKey("sponsor") 
				&& (papaya_json.get("sponsor") instanceof String)
				&& (sponsor = (String) papaya_json.get("sponsor")) != null) {
			
			if (sponsor.length() > 45) {
				sponsor = sponsor.substring(0, 45);
			}
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
		session_id = UIDGenerator.generateUID(user_id);
		boolean exists = false;
		Connection con = getRemoteConnection(context);

		try {

			for (int i = 0; (exists = sessionIDExists(session_id, con)) && i < 3; i++) {
				//generate a session_id with user_id as the salt
				session_id = UIDGenerator.generateUID(user_id);
			}
			if (exists) {
				logger.log(
						"ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times. Try recalling.");
			}

			String insertSession = "INSERT INTO sessions VALUES ('" + session_id + "', '" 
						+ user_id + "', " + duration + ", '" + location_desc + "', '" 
						+ description + "', '" + sponsor + "', " + location_lat.floatValue() 
						+ ", " + location_long.floatValue() + ", '" + startTime + "')";
			Statement statement = con.createStatement();
			statement.execute(insertSession);
			statement.close();
			
			String insertClassSession = "INSERT INTO classes_sessions VALUES (" + active + ", '" + session_id + "', '" + class_id + "')";
			statement = con.createStatement();
			statement.execute(insertClassSession);
			statement.close();

			String insertUserSession = "INSERT INTO users_sessions VALUES (" + active + ", '" + user_id + "', '" + session_id + "')";
			statement = con.createStatement();
			statement.execute(insertUserSession);
			statement.close();
			
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
