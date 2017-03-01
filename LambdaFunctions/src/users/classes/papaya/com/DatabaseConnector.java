package users.classes.papaya.com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.amazonaws.services.s3.event.*;
import com.amazonaws.services.s3.model.S3Event;

public class DatabaseConnector {
	static final String RDS_HOSTNAME = "rds-mysql-papaya.cupvols8ubdm.us-west-2.rds.amazonaws.com";
	static final String RDS_PORT = "3306";
	static final String RDS_DB_NAME = "papaya_db";
	static final String RDS_USERNAME = "teamsixadmin";
	static final String RDS_PASSWORD = "papaya46";
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

	public static void main(String[] args) {
		//insertUser(2, "testUser2", "3171345343", "blahblah@gmail.com", 1354342, 234);
		System.out.println("Starting");
		Connection con = getRemoteConnection();

		try {
			String insertUser = "INSERT INTO users VALUES (1, 'testUser', 3178161439, 'testemail@gmail.com', 12413513, 143)";
			/*String insertUser = "INSERT INTO users VALUES (" + args[0] + ", " + args[1] + ", " + args[2] + ", "
					+ args[3] + ", " + args[4] + ", " + args[5] + ")";*/
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		} finally {
			System.out.println("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
				}
		}

	}

	public static void insertUser(int user_id, String username, String phone, String email, int authentication_key,
			int current_session_id) {
		System.out.println("Starting");
		Connection con = getRemoteConnection();
		try {
			// String insertUser = "INSERT INTO users VALUES (1, 'testUser',
			// 3178161439, 'testemail@gmail.com', 12413513, 143)";
			String insertUser = "INSERT INTO users VALUES (" + user_id + ", " + "'" + username + "', " + phone + ", "
					+ "'" + email + "', " + authentication_key + ", " + current_session_id + ")";
			Statement statement = con.createStatement();
			statement.addBatch(insertUser);
			statement.executeBatch();
			statement.close();

		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		} finally {
			System.out.println("Closing the connection.");
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
				}
		}
	}

	public static Connection getRemoteConnection() {
		try {
			Class.forName(JDBC_DRIVER);
			String dbName = RDS_DB_NAME;
			String userName = RDS_USERNAME;
			String password = RDS_PASSWORD;
			String hostname = RDS_HOSTNAME;
			String port = RDS_PORT;
			String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password="
					+ password;
			System.out.println(JDBC_DRIVER + " " + jdbcUrl);
			Connection con = DriverManager.getConnection(jdbcUrl);
			if (con == null) {
				System.out.println("Connection is null");
			}
			System.out.println("Success");
			return con;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
