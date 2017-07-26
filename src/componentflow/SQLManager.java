package componentflow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLManager {

	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	private static String delFlowTableIfExists = "DROP TABLE IF EXISTS cflow.%s%s%s;";
	private static String createFlowTable = "CREATE TABLE cflow.%s%s%s (source VARCHAR(300) NOT NULL,sink VARCHAR(300) NOT NULL,source_caller VARCHAR(200) NOT NULL,sink_caller VARCHAR(200) NOT NULL,PRIMARY KEY (`source`, `sink`, `source_caller`, `sink_caller`));";
	private static String insertFlow = "INSERT INTO cflow.%s%s%s (source, sink, source_caller, sink_caller) VALUES (?, ?, ?, ?);";

	public SQLManager() throws ClassNotFoundException {
		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://localhost/cflow?" + "user=cflow&password=cflow"
					+ "&verifyServerCertificate=false&useSSL=true");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	// Create table with given name
	public void createFlowTable(String appname) {
		try {
			// Stackoverflow sanitization for table name
			java.sql.DatabaseMetaData md = connect.getMetaData();
			String q = md.getIdentifierQuoteString();
			String query1 = String.format(delFlowTableIfExists, q, appname.replaceAll(q, q + q), q);
			String query2 = String.format(createFlowTable, q, appname.replaceAll(q, q + q), q);
			// Drop table if it exists and create table with given name
			statement = connect.createStatement();
			statement.executeUpdate(query1);
			statement.executeUpdate(query2);
			statement.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void addFlow(String appname, String source, String sink, String sourceCaller, String sinkCaller) {
		try {
			// Stackoverflow sanitization for table name
			java.sql.DatabaseMetaData md = connect.getMetaData();
			String q = md.getIdentifierQuoteString();
			String query = String.format(insertFlow, q, appname.replaceAll(q, q + q), q);
			// Insert given flow into given app's table
			preparedStatement = connect.prepareStatement(query);
			preparedStatement.setString(1, source);
			preparedStatement.setString(2, sink);
			preparedStatement.setString(3, sourceCaller);
			preparedStatement.setString(4, sinkCaller);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void analyseFlows(String app1, String app2) {
		// TODO match flows
	}

	// You need to close the resultSet
	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

}