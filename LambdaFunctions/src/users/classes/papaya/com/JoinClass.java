package users.classes.papaya.com;

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

public class JoinClass implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	/** Steps to implement a generic papaya API Lambda Function:
	 * 
	 * 	1.	Check request body (validate) for proper format of fields.
	 * 	2.	Ensure request is authentic from authentication service.
	 * 	3a.	Get any data from tables to complete request.
	 * 	3b.	Change tables as necessary for particular request.
	 * 
	 * 	For description of the API requirements, seek the API Documentation in developers folder.
	 */

	// TODO: MAKE SURE WE DON'T REJOIN THE SAME CLASS WE'RE ALREADY IN (JOIN 2+ TIMES).
	
	private Context context;
	private LambdaLogger logger;

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		
		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> json;
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;
		// Required request fields:
		String user_id, authentication_key, service, access_key;
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
			
			// 1. validate 'user_id' field is of length allowed in database, otherwise truncate.
			user_id = Validate.user_id(json);
			// 2. validate 'service' and check if it matches defined service types. 
			service_type = Validate.service(json);
			// 3. validate 'authentication_key' is of length allowed?
			// TODO: Determine more strict intro rules
			authentication_key = Validate.authentication_key(json, service_type);
			// 4. validate 'access_key' is of right length
			access_key = Validate.access_key(json);
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
			
			//get the class_id from database
			String get_class_id = "SELECT class_id, student_access_key, ta_access_key, professor_access_key FROM classes WHERE student_access_key='" + access_key + "' OR ta_access_key='" + access_key + "' OR professor_access_key='" + access_key +"'";
			ResultSet class_id_rs = statement.executeQuery(get_class_id);
			logger.log("query = " + get_class_id + "\n");
			String class_id = "";
			int user_role = -1;
			if (class_id_rs.next()) {
				if (!class_id_rs.isLast()) {
					logger.log("\nERROR: 500 Internal Server Error - There are multiple rows with same access key.\n");
					return generate500("There are multiple rows with the same access key.");
				}
				
				// if it is returned from the database, it should be formatted correctly and not null
				class_id = class_id_rs.getString("class_id");
				String student_access_key = class_id_rs.getString("student_access_key");
				String ta_access_key = class_id_rs.getString("ta_access_key");
				String professor_access_key = class_id_rs.getString("professor_access_key");
				if (access_key.equals(professor_access_key))
					user_role = UserRole.value(UserRole.PROFESSOR);
				else if (access_key.equals(ta_access_key))
					user_role = UserRole.value(UserRole.TA);
				else if (access_key.equals(student_access_key))
					user_role = UserRole.value(UserRole.STUDENT);
				else {
					logger.log("ERROR: 400 Bad Request - Returned to client. user_role is not a value [1-3].");
					return generate400("user_role is not a value [1-3].", "user_role");
				}
			}
			else {
				logger.log("ERROR: 400 Bad Request - Returned to client. class_id does not exist from given access_key.");
				return generate400("class_id does not exist, probably from bad access_key.", "class_id");
			}
			
			// Insert new row into users_classes
			String joinClass = "INSERT INTO users_classes VALUES (" + user_role + ", '"
					+ user_id + "', '" + class_id + "')";
			
			statement.execute(joinClass);
			statement.executeBatch();
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
