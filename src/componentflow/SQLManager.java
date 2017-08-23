package componentflow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.efabrika.util.DBTablePrinter;

public class SQLManager {

	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	// Quote character
	private String q = null;

	private static String delFlowTableIfExists = "DROP TABLE IF EXISTS cflow.%s%s%s;";
	private static String createFlowTable = "CREATE TABLE cflow.%s%s%s (source VARCHAR(300) NOT NULL,sink VARCHAR(300) NOT NULL,source_caller VARCHAR(200) NOT NULL,sink_caller VARCHAR(200) NOT NULL,PRIMARY KEY (`source`, `sink`, `source_caller`, `sink_caller`));";
	private static String insertFlow = "INSERT INTO cflow.%s%s%s (source, sink, source_caller, sink_caller) VALUES (?, ?, ?, ?);";

	private static String createSemanticsTable = "CREATE TABLE IF NOT EXISTS cflow.semantics (sink VARCHAR(300) NOT NULL,source VARCHAR(300) NOT NULL,name VARCHAR(300) NOT NULL,PRIMARY KEY (`sink`, `name`, `source`));";
	private static String insertSemantic = "INSERT INTO cflow.semantics (sink,source,name) VALUES (?, ?, ?);";

	private static String cleanViews = "DROP VIEW IF EXISTS `phonetosemantics`, `semanticstowear`, `phonesemanticjoinwear`, `phonesemanticjoinweartosemantics`, `semanticstophone`, `phonesemanticjoinwearsemanticjoinphone`;";

	private static String createViewPhoneToSemantics = "CREATE VIEW `phonetosemantics` AS "
			+ "SELECT cflow.%s%s%s.`source` AS `phonesource`, `semantics`.`name` AS `name` "
			+ "FROM (cflow.%s%s%s JOIN `semantics` ON ((cflow.%s%s%s.`sink` = `semantics`.`sink`)));";

	private static String createViewSemanticsToWear = "CREATE VIEW `semanticstowear` AS "
			+ "SELECT cflow.%s%s%s.`sink` AS `wearsink`, `semantics`.`name` AS `name` "
			+ "FROM (cflow.%s%s%s JOIN `semantics` ON ((cflow.%s%s%s.`source` = `semantics`.`source`)));";

	private static String createViewPhoneSemanticJoinWear = "CREATE VIEW `phonesemanticjoinwear` AS "
			+ "SELECT `phonetosemantics`.`phonesource` AS `phonesource`, `phonetosemantics`.`name` AS `api`, `semanticstowear`.`wearsink` AS `wearsink` "
			+ "FROM (`phonetosemantics` JOIN `semanticstowear` ON ((`phonetosemantics`.`name` = `semanticstowear`.`name`)));";

	private static String createViewPhoneSemanticJoinWearToSemantics = "CREATE VIEW `phonesemanticjoinweartosemantics` AS "
			+ "SELECT `phonesemanticjoinwear`.`phonesource` AS `phonesource`, `phonesemanticjoinwear`.`wearsink` AS `wearsink`, `semantics`.`name` AS `name` "
			+ "FROM (`phonesemanticjoinwear` JOIN `semantics` ON ((`phonesemanticjoinwear`.`wearsink` = `semantics`.`sink`)));";

	private static String createViewSemanticsToPhone = "CREATE VIEW `semanticstophone` AS "
			+ "SELECT cflow.%s%s%s.`sink` AS `phonesink`, `semantics`.`name` AS `name` "
			+ "FROM (cflow.%s%s%s JOIN `semantics` ON ((cflow.%s%s%s.`source` = `semantics`.`source`)));";

	private static String createViewPhoneSemanticJoinWearSemanticJoinPhone = "CREATE VIEW `phonesemanticjoinwearsemanticjoinphone` AS "
			+ "SELECT `phonesemanticjoinweartosemantics`.`phonesource` AS `phonesource`, `phonesemanticjoinweartosemantics`.`name` AS `api`, `semanticstophone`.`phonesink` AS `phonesink` "
			+ "FROM (`phonesemanticjoinweartosemantics` JOIN `semanticstophone` ON ((`phonesemanticjoinweartosemantics`.`name` = `semanticstophone`.`name`)));";

	public SQLManager() throws ClassNotFoundException, SQLException {
		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		// Setup the connection with the DB
		connect = DriverManager.getConnection("jdbc:mysql://localhost/cflow?" + "user=cflow&password=cflow"
				+ "&verifyServerCertificate=false&useSSL=true");
		// Get quote character
		java.sql.DatabaseMetaData md = connect.getMetaData();
		q = md.getIdentifierQuoteString();
	}

	// Create table with given name
	public void createFlowTable(String appname) {
		try {
			// Sanitization for table name
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
			// Sanitization for table name
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

	public void analyseFlows(String packagename) {
		analyseFlows("phone" + packagename, "wear" + packagename);
	}

	public void analyseFlows(String app1, String app2) {
		app1 = app1.replaceAll(q, q + q);
		app2 = app2.replaceAll(q, q + q);
		try {
			// Prepare all queries with input name (and sanitise)
			String createViewPhoneToSemanticsF = String.format(createViewPhoneToSemantics,
					q, app1, q,
					q, app1, q,
					q, app1, q);
			String createViewSemanticsToWearF = String.format(createViewSemanticsToWear,
					q, app2, q,
					q, app2, q,
					q, app2, q);
			String createViewSemanticsToPhoneF = String.format(createViewSemanticsToPhone,
					q, app1, q,
					q, app1, q,
					q, app1, q);
			// Drop table if it exists and create table with given name
			statement = connect.createStatement();
			statement.executeUpdate(cleanViews);
			// Source on phone -> msgAPI
			statement.executeUpdate(createViewPhoneToSemanticsF);
			// msgAPI -> sink on wear
			statement.executeUpdate(createViewSemanticsToWearF);
			// Source on phone -> msgAPI -> sink on wear
			statement.executeUpdate(createViewPhoneSemanticJoinWear);
			// Sink on wear => msgAPI
			statement.executeUpdate(createViewPhoneSemanticJoinWearToSemantics);
			// msgAPI -> sink on phone
			statement.executeUpdate(createViewSemanticsToPhoneF);
			// Sink on wear => msgAPI -> sink on phone
			statement.executeUpdate(createViewPhoneSemanticJoinWearSemanticJoinPhone);
			statement.close();
			
			DBTablePrinter.printTable(connect, "phonesemanticjoinwear");
			DBTablePrinter.printTable(connect, "phonesemanticjoinwearsemanticjoinphone");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void addSemantic(String sink, String source, String name) {
		try {
			statement = connect.createStatement();
			statement.executeUpdate(createSemanticsTable);
			statement.close();
			preparedStatement = connect.prepareStatement(insertSemantic);
			preparedStatement.setString(1, sink);
			preparedStatement.setString(2, source);
			preparedStatement.setString(3, name);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	// You need to close the resultSet
	public void close() {
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