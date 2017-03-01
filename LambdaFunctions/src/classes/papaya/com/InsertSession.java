package classes.papaya.com;

import static utils.papaya.com.ResponseGenerator.throw400;
import static utils.papaya.com.ResponseGenerator.throw500;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import users.papaya.com.CreateUser.SERVICE_TYPE;
import utils.papaya.com.AuthServiceType;
import utils.papaya.com.Authentication;
import utils.papaya.com.UIDGenerator;

public class InsertSession implements RequestHandler<Map<String, Object>, String> {
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

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

		this.context = context;
		this.logger = context.getLogger();
		Map<String, Object> response = new HashMap<String, Object>();
		AuthServiceType service_type = AuthServiceType.NONE;

		// Required request fields for authentication:
		String authentication_key, service;

		// Required request fields for SQL
		String session_id, location_desc;
		String user_id = "";
		Integer duration;
		Float location_lat, location_long;
		// Optional request fields:
		String sponsor;

		/*
		 * 1. Check request body (validate) for proper format of fields:
		 * 
		 * fields must exist: user_id authentication_key service
		 * 
		 * // TODO: Check for SQL INJECTION!
		 */

		// Check for required key
		if ((!input.containsKey("user_id") || !(input.get("user_id") instanceof String)
				|| !((user_id = (String) input.get("user_id")) != null))
				|| (!input.containsKey("authentication_key") || !(input.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) input.get("authentication_key")) != null))
				|| (!input.containsKey("service") || !(input.get("service") instanceof String)
						|| !((service = (String) input.get("service")) != null))) {

			// TODO: Add "fields" that were actually the problem.
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("user_id or authentication_key or service do not exist.", "");
		}
		if (!input.containsKey("duration") 
				|| !(input.get("duration") instanceof String)
				|| !((duration = (Integer) input.get("duration")) != null)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("Duration does not exist", "");
		}
		if (!input.containsKey("location_lat") 
				|| !(input.get("location_lat") instanceof String)
				|| !((location_lat = (Float) input.get("location_lat")) != null)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("location_lat does not exist", "");
		}
		if (!input.containsKey("location_long") 
				|| !(input.get("location_long") instanceof String)
				|| !((location_lat = (Float) input.get("location_long")) != null)) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("location_long does not exist.", "");
		}
		if ((!input.containsKey("session_id") 
				|| !(input.get("session_id") instanceof String)
				|| !((user_id = (String) input.get("session_id")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("session_id does not exist.", "");
		}
		if ((!input.containsKey("location_desc") 
				|| !(input.get("location_desc") instanceof String)
				|| !((user_id = (String) input.get("location_desc")) != null))) {
			logger.log("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.");
			return throw400("location_desc does not exist.", "");
		}
		
		// Check for proper formatting of supplied elements. Check by field.
		
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


		// 5. validate 'email' is of acceptable format and length if it exists.
		if (input.containsKey("sponsor") && (input.get("sponsor") instanceof String)) {

			sponsor = (String) input.get("sponsor");

			if (sponsor.length() > 45) {

				logger.log(
						"ERROR: 400 Bad Request - Returned to client. email was not formatted right (i.e. length or no @ or no domain) '"
								+ sponsor + "'.");
				return throw400("sponsor was not formatted right (length > 45) '" + sponsor + "'.",
						"sponsor");
			}
		}

		/*
		 * 2. Authentic authentication_key check:
		 * 
		 */
		// TODO: actually check authentication service here.

		/*
		 * 3a. Get any data from tables to complete request.
		 * 
		 * 1. Check if
		 */

		/*
		 * ### Generate unique user_id number and validate its uniqueness.
		 */
		session_id = UIDGenerator.generateUID(session_id);
		boolean exists = false;
		Connection con = getRemoteConnection(context);

		try {

			for (int i = 0; (exists = userIDExists(user_id, con)) && i < 3; i++) {
				user_id = UIDGenerator.generateUID(username);
			}
			if (exists) {
				logger.log(
						"ERROR: 500 Internal Server Error - Returned to client. Could not generate a UID on 3 tries.");
				return throw500("generateUID() failed 3 times. Try recalling.");
			}

			String insertSession = "INSERT INTO users VALUES ('" + user_id + "', '" + username + "', " + phone + ", "
					+ "'" + email + "', '" + authentication_key + "', '" + "" + "')";
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			logger.log("SQLException: " + ex.getMessage());
			logger.log("SQLState: " + ex.getSQLState());
			logger.log("VendorError: " + ex.getErrorCode());

			return throw500("SQL error.");

		} finally {
			context.getLogger().log("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					return throw500("SQL error.");
				}
		}

		response.put("code", 201);
		response.put("code_description", "Created");
		response.put("user_id", user_id);
		response.put("authentication_key", authentication_key);
		return response;
	}

}
