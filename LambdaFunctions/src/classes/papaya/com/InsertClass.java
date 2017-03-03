package classes.papaya.com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InsertClass implements RequestHandler<Map<String, Object>, String> {

	@Override
	public String handleRequest(Map<String, Object> input, Context context) {
		context.getLogger().log("Input: " + input + "\n");

		// TODO: implement your handler
		context.getLogger().log("before connection established\n");
		Connection con = getRemoteConnection(context);
		context.getLogger().log("after connection established\n");
		try {

			String insertClass = "INSERT INTO classes VALUES (" + input.get("class_id") + ", " + "'"
					+ input.get("classname") + "', " + input.get("user_access_key") + ", " + input.get("ta_access_key")
					+ ", " + input.get("professor_access_key") + ", '" + input.get("description") + "')";
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
		return null;
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
