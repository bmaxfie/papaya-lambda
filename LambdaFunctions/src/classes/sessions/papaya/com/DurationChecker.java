package classes.sessions.papaya.com;

import static utils.papaya.com.ResponseGenerator.generate404;
import static utils.papaya.com.ResponseGenerator.generate500;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.text.DateFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class DurationChecker implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	
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
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//String current_time = dateFormatter.format(new Date());
		Date current_time = new Date();
		
		Connection con = getRemoteConnection(context);

		try {
			
			String getActiveSessions = "SELECT user_session_id FROM users_sessions WHERE active=1";
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(getActiveSessions);
			
			HashSet<String> activeSessions = new HashSet<String>();
			while (result.next()) {
				String session_id = result.getString("user_session_id");
				if (!activeSessions.contains(session_id))
					activeSessions.add(session_id);
			}
			result.close();
			statement.close();

			if (activeSessions.isEmpty())
				return new HashMap<String, Object>();
			
			String getSessionInfo = "SELECT session_id, start_time, duration FROM sessions WHERE session_id IN ( ";
			for (String id : activeSessions) {
				getSessionInfo += "'" + id + "', ";
			}
			if (!activeSessions.isEmpty())
				getSessionInfo = getSessionInfo.substring(0, getSessionInfo.length() - 2) + " )";
			logger.log("\ngetSessionInfo: " + getSessionInfo + "\n");
			statement = con.createStatement();
			result = statement.executeQuery(getSessionInfo);
			
			HashSet<String> sessionInactives = new HashSet<String>();
			while (result.next()) {
				try {
					Date date = dateFormatter.parse(result.getString("start_time"));
					//logger.log("session_id: " + result.getString("session_id") + " start_time: " + result.getString("start_time") + " toInstant: " + date.toInstant());
					if (date.toInstant().plus(result.getInt("duration"), ChronoUnit.MINUTES).isBefore(current_time.toInstant())) {
						// make session inactive
						String id = result.getString("session_id");
						if (!sessionInactives.contains(id)) {
							sessionInactives.add(id);
						}
					}
				} catch (ParseException ignore) { logger.log("PARSE EXCEPTION: " + ignore.getMessage()); }
			}
			result.close();
			statement.close();

			logger.log("inactives: " + sessionInactives.toString() + "\n");
			
			if (sessionInactives.isEmpty())
				return new HashMap<String, Object>();
			
			String updateInactives = "UPDATE users_sessions SET active=0 WHERE user_session_id IN ( ";
			for (String id : sessionInactives) {
				updateInactives += "'" + id + "', ";
			}
			updateInactives = updateInactives.substring(0, updateInactives.length() - 2) + " )";
			logger.log("updateInactives: " + updateInactives + "\n");
			statement = con.createStatement();
			statement.execute(updateInactives);
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

		return new HashMap<String, Object>();
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
