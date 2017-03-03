package classes.papaya.com;

import static utils.papaya.com.ResponseGenerator.throw400;
import static utils.papaya.com.ResponseGenerator.throw404;
import static utils.papaya.com.ResponseGenerator.throw500;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InsertClass implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	private Context context;
	private LambdaLogger logger;

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> papaya_json = (Map<String, Object>) input.get("body-json");
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;

		// Required request fields for authentication:
		String authentication_key, service;
		String user_id = "";

		// Required request fields for SQL
		String class_id, classname, student_access_key, ta_access_key, professor_access_key;

		// Optional request fields:
		String description = "";
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 
		 * fields must exist: user_id authentication_key service
		 * 
		 * // TODO: Check for SQL INJECTION!
		 */

		// Check for required key
		if ((!papaya_json.containsKey("user_id") || !(papaya_json.get("user_id") instanceof String)
				|| !((user_id = (String) papaya_json.get("user_id")) != null))
				|| (!papaya_json.containsKey("authentication_key")
						|| !(papaya_json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) papaya_json.get("authentication_key")) != null))
				|| (!papaya_json.containsKey("service") || !(papaya_json.get("service") instanceof String)
						|| !((service = (String) papaya_json.get("service")) != null))) {

			// TODO: Add "fields" that were actually the problem.
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("user_id or authentication_key or service do not exist.", "");
		}
		
		if ((!papaya_json.containsKey("classname") 
				|| !(papaya_json.get("classname") instanceof String)
				|| !((classname = (String) papaya_json.get("classname")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("classname does not exist.", "");
		}
		
		if ((!papaya_json.containsKey("description") 
				|| !(papaya_json.get("description") instanceof String)
				|| !((description = (String) papaya_json.get("description")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("description does not exist.", "");
		}

		// 2. validate 'service' field is a recognizable type
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			logger.log("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service
					+ "'.");
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
			logger.log(
					"ERROR: 400 Bad Request - Returned to client. authentication_key was not of valid length, instead it was '"
							+ authentication_key.length() + "'.");
			return throw400(
					"authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.",
					"authentication_key");
		}

		// 5. validate 'sponsor' is of acceptable format and length if it exists.
		if (papaya_json.containsKey("description") 
				&& (papaya_json.get("description") instanceof String)
				&& (description = (String) papaya_json.get("description")) != null) {
					
			if (description.length() > 255) {
				description = description.substring(0, 255);
			}
		}
		
		class_id = UIDGenerator.generateUID(classname);
		student_access_key = UIDGenerator.generateUID(classname);
		ta_access_key = UIDGenerator.generateUID(classname);
		professor_access_key = UIDGenerator.generateUID(classname);
		
		boolean exists = false;
		Connection con = getRemoteConnection(context);
		
		
		try {
			if(!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist");
				return throw404("user_id not found");
			}
			
			for (int i = 0; (exists = classIDExists(class_id, con)) && i < 3; i++) {
				class_id = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return throw500("generateUID() failed 3 times. Try recalling.");
			}
			
			for (int i = 0; (exists = studentKeyExists(student_access_key, con)) && i < 3; i++) {
				student_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return throw500("generateUID() failed 3 times (student_access_key). Try recalling.");
			}
			
			for (int i = 0; (exists = taKeyExists(ta_access_key, con)) && i < 3; i++) {
				ta_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return throw500("generateUID() failed 3 times (ta_access_key). Try recalling.");
			}
			
			for (int i = 0; (exists = professorKeyExists(professor_access_key, con)) && i < 3; i++) {
				professor_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return throw500("generateUID() failed 3 times (professor_access_key). Try recalling.");
			}
			
			String insertUser = "INSERT INTO classes VALUES (" + input.get("class_id") + ", " + "'"
					+ input.get("classname") + "', " + input.get("user_access_key") + ", " + input.get("ta_access_key")
					+ ", " + input.get("professor_access_key") + ", " + input.get("description") + ")";
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			context.getLogger().log("SQLException: " + ex.getMessage());
			context.getLogger().log("SQLState: " + ex.getSQLState());
			context.getLogger().log("VendorError: " + ex.getErrorCode());
		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
				}
		}
		return null;
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
	
	private boolean classIDExists(String class_id, Connection dbcon) throws SQLException
    {
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
	
	private boolean studentKeyExists(String student_access_key, Connection dbcon) throws SQLException
    {
		String getStudentKey = "SELECT student_access_key FROM classes WHERE student_access_key='"+student_access_key+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getStudentKey);
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
	
	private boolean taKeyExists(String ta_access_key, Connection dbcon) throws SQLException
    {
		String getTAKey = "SELECT ta_access_key FROM classes WHERE ta_access_key='"+ta_access_key+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getTAKey);
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
	
	private boolean professorKeyExists(String professor_access_key, Connection dbcon) throws SQLException
    {
		String getProfKey = "SELECT professor_access_key FROM classes WHERE professor_access_key='"+professor_access_key+"'";
		Statement statement = dbcon.createStatement();
		ResultSet result = statement.executeQuery(getProfKey);
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
