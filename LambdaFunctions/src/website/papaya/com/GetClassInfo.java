package website.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate500;

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

import utils.papaya.com.Exception400;
import utils.papaya.com.ResponseGenerator;
import utils.papaya.com.Validate;

public class GetClassInfo implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
		Map<String, Object> querystring;
		// Required request fields:
		String professor_access_key;
		
		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 		
		 * 		fields must exist:
		 * 			access_key
		 * 
		 * 		// TODO: Check for SQL INJECTION!
		 */
		
		
		try {
			// Find Path:
			querystring = Validate.field(input, "params");
			querystring = Validate.field(querystring, "querystring");
			
			// Validate required fields:
			professor_access_key = Validate.access_key(querystring);
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
		}
		
		/*
		 * 3a. Get any data from tables to complete request.
		 * 		
		 * 		1. Check if 
		 */
		Connection con = getRemoteConnection(context);
		
		try {
			
			String getClassInfo = "SELECT * FROM classes WHERE professor_access_key='" + professor_access_key + "'";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getClassInfo);
			if (result.next() && result.isLast()) {
				response.put("class_id", result.getString("class_id"));
				response.put("classname", result.getString("classname"));
				response.put("student_access_key", result.getString("student_access_key"));
				response.put("ta_access_key", result.getString("ta_access_key"));
				response.put("professor_access_key", result.getString("professor_access_key"));
				response.put("description", result.getString("description"));
			}
			else {
				logger.log("Either there are multiple results under single access key or no result for access key.");
				return ResponseGenerator.generate404("Either there are multiple classes under that access key or there is no class for that access key.");
			}
			
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
					return generate500("SQL error.");
				}
		}
		
		response.put("code", 200);
		response.put("code_description", "OK");
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
