package posts.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Exception400;
import utils.papaya.com.Validate;

public class GetPosts implements RequestHandler<Map<String, Object>, Map<String, Object>>{

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
		String session_id = "";
		
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

			String getPosts = "SELECT * FROM posts WHERE post_session_id='" + session_id + "'";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getPosts);
			
			ArrayList<Map<String, Object>> posts = new ArrayList<Map<String, Object>>();
			
			while (result.next()) {
				Map<String, Object> post = new HashMap<String, Object>();
				post.put("post_id", result.getString("post_id"));
				post.put("post_user_id", result.getString("post_user_id"));
				post.put("post_user_role", result.getString("post_user_role"));
				post.put("timestamp", result.getString("timestamp"));
				post.put("message", result.getString("message"));
				post.put("visibility", result.getString("visibility"));
				posts.add(post);
			}
			
			Collections.sort(posts, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
					SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						Date d1 = dateFormatter.parse((String) arg0.get("timestamp"));
						Date d2 = dateFormatter.parse((String) arg1.get("timestamp"));
						return d1.compareTo(d2);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
					return 0;
				}
			});
			
			response.put("posts", posts);
			
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
