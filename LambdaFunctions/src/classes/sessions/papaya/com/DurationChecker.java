package classes.sessions.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate404;
import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class DurationChecker implements RequestHandler<String, String> {
	
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
	public String handleRequest(String input, Context context) {

		this.context = context;
		this.logger = context.getLogger();
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Connection con = getRemoteConnection(context);

		try {
			
			//TODO need to check if current_session_id matches a session_id in the sessions or user_sessions table?
			
			String removeUser = "SELECT user_session_id FROM user_sessions WHERE ";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(removeUser);
			statement.close();
		
//			//removes the user-session pair from the users-sessions table
//			String removeUser = "DELETE FROM users_sessions WHERE session_user_id='" + user_id + "' AND user_session_id='" + current_session_id + "'";
//			statement = con.createStatement();
//			statement.execute(removeUser);
//			statement.close();
//			
			//sets the current session of the user back to nothing as they are no longer in a session
			String updateUser = "UPDATE users SET current_session_id='' WHERE user_id='" + user_id + "'";
			statement = con.createStatement();
			statement.execute(updateUser);
			statement.close();
			
			
		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
				}
		}

		return "";
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
