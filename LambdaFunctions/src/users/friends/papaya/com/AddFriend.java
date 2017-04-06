package users.friends.papaya.com;

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
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class AddFriend implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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
		String authentication_key, service_user_id;
		String user_id = ""; //user_id of sender of friend request/confirmation
		String user_id2 = ""; //user_id of receiver of friend request/confirmation

		// Required request fields for SQL
		try {
			// Find path:
			json = Validate.field(input, "body_json");
			
			// Validate required fields:
			user_id = Validate.user_id(json);
			user_id2 = Validate.user_id2(json);
			service_type = Validate.service(json);
			authentication_key = Validate.authentication_key(json, service_type);
			service_user_id = Validate.service_user_id(json, service_type);
			
		} catch (Exception400 e400) {
			logger.log(e400.getMessage());
			return e400.getResponse();
		}
		
		boolean exists = false;
		Connection con = getRemoteConnection(context);
		
		
		try {
			if(!userIDExists(user_id, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist");
				return generate404("user_id not found");
			}
			if(!userIDExists(user_id2, con)) {
				logger.log("ERROR: 404 Not Found - user_id does not exist");
				return generate404("user_id2 not found");
			}
			
			/*
			 * if friends entry exists already, update confirmed to true/1
			 * otherwise add entry to the table with confirmed=false/0
			 */
			String entryExistsQuery = "SELECT friend_sender_id FROM friends "
					+ "WHERE ( friend_sender_id='" + user_id + "' AND friend_receiver_id='" + user_id2 + "') "
					+ "OR ( friend_sender_id='" + user_id2 + "' AND friend_receiver_id='" + user_id + "')";
			Statement statement = con.createStatement();
			ResultSet entryExistsResult = statement.executeQuery(entryExistsQuery);
			
			boolean entryExists;
			if(entryExistsResult.next()) {
				entryExists = true;
			} else {
				entryExists = false;
			}
			entryExistsResult.close();
			statement.close();
			
			String addFriend = "INSERT INTO friends VALUES ('" + user_id + "', '" + user_id2 + "')";
			statement = con.createStatement();
			if(!entryExists) {
				statement.execute(addFriend);
			}
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
	
	}
