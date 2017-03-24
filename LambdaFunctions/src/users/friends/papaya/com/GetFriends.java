package users.friends.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate400;
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
import com.amazonaws.services.stepfunctions.builder.states.NextStateTransition;

import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Authentication;
import utils.papaya.com.Exception400;
import utils.papaya.com.Validate;

public class GetFriends implements RequestHandler<Map<String, Object>, Map<String, Object>>{

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
		String user_id = "", authentication_key = "";
		
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
			
			// 1. validate 'user_id'
			user_id = Validate.user_id(querystring);
			// 2. validate 'service' field is a recognizable type
			service_type = Validate.service(querystring);				
			// 3. validate 'authentication_key' is of length allowed?
			// TODO: Determine more strict intro rules
			authentication_key = Validate.authentication_key(querystring, service_type);
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
			
			/*
			 * TODO: NEW SQL BELOW
			 * TODO: only returns the user_id not the username
			 * 
			 * SQL command:
			 * 
			 * select users.username from 
			 * users
			 * inner join 
			 * (
			 * 		select friend_receiver_id from friends WHERE confirmed=1 AND friend_sender_id='A'
			 * 		union
			 * 		select friend_sender_id from friends WHERE confirmed=1 AND friend_receiver_id='A'
			 * ) friendId
			 * ON (friendId.friend_receiver_id=users.user_id);
			 */
			
			String getclassids = "SELECT user_class_id FROM users_classes WHERE class_user_id='"+user_id+"'";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getclassids);
			
			ArrayList<String> class_ids = new ArrayList<String>();
			while (result.next()) {
				class_ids.add(result.getString(1));
			}
			response.put("class_ids", class_ids.toArray());
			
			result.close();
			statement.close();

			// TODO: Get other information about classes, like their names.
			String getclassnames = "SELECT * FROM classes WHERE";
			for (int i = 0; i < class_ids.size(); i++) {
				if (i == class_ids.size() - 1)
					getclassnames += " class_id='"+class_ids.get(i)+"'";
				else
					getclassnames += " class_id='"+class_ids.get(i)+"' OR";
			}
			logger.log(getclassnames);
			
			statement = con.createStatement();
			result = statement.executeQuery(getclassnames);
			logger.log("class_ids size: " + class_ids.size() + "\n");
			String[] class_names = new String[class_ids.size()];
			int index = 0;
			while (result.next()) {
				index = class_ids.indexOf(result.getString("class_id"));
				logger.log(index + "\n");
				class_names[index] = result.getString("classname");
			}
			response.put("classnames", class_names);

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
