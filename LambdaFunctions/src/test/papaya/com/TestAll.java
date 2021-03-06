package test.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Exception400;
import utils.papaya.com.Validate;

public class TestAll implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
	
	/**
	 * This function TestAll will test the following Lambda Functions.
	 * 
	 * We will do our best to make sure this tests all currently implemented functions.
	 * 
	 * In order to test this function you will need to pass in three variables:
	 * 		service - either GOOGLE or FACEBOOK
	 * 		service_user_id - the user id returned from the service specified
	 * 		authentication_key - the token id returned from the service specified
	 * 
	 * These will need to be valid upon the time this function is called. They do not need
	 * to be tied to any user within our database.
	 */
	
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
		String service, authentication_key, service_user_id;

		
		try {
			json = Validate.field(input, "body-json");
			
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
		}
		
		
		/*
		 * 1. Insert Class
		 */
		
		
		/*
		 * Setup database rows to facilitate further tests (create test users and classes, etc.)
		 */
		Connection con = getRemoteConnection(context);
		
		try {
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
