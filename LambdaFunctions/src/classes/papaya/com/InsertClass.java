package classes.papaya.com;

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
import static utils.papaya.com.ResponseGenerator.*;


public class InsertClass implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	private Context context;
	private LambdaLogger logger;

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> json;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;

		// Required request fields for authentication:
		String authentication_key;
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
		
		try {
			// Find path:
			json = Validate.field(input, "body-json");
			
			// Validate required fields:
			user_id = Validate.user_id(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			classname = Validate.classname(json);
			description = Validate.description(json, "description");
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
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
				return generate404("user_id not found");
			}
			
			for (int i = 0; (exists = classIDExists(class_id, con)) && i < 3; i++) {
				class_id = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times. Try recalling.");
			}
			
			for (int i = 0; (exists = studentKeyExists(student_access_key, con)) && i < 3; i++) {
				student_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times (student_access_key). Try recalling.");
			}
			
			for (int i = 0; (exists = taKeyExists(ta_access_key, con)) && i < 3; i++) {
				ta_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times (ta_access_key). Try recalling.");
			}
			
			for (int i = 0; (exists = professorKeyExists(professor_access_key, con)) && i < 3; i++) {
				professor_access_key = UIDGenerator.generateUID(classname);
			}
			if (exists) {
				logger.log("ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return generate500("generateUID() failed 3 times (professor_access_key). Try recalling.");
			}
			
			String insertClass = "INSERT INTO classes VALUES ('" + class_id + "', " + "'"
					+ classname + "', '" + student_access_key + "', '" + ta_access_key
					+ "', '" + professor_access_key + "', '" + description + "')";
			
            Statement statement = con.createStatement();
			statement.addBatch(insertClass);
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
		
		response.put("code", 201);
		response.put("code_description", "Created");
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
