package componentflow;

import java.io.IOException;
import java.sql.SQLException;

import soot.jimple.infoflow.android.TestApps.TestCompat;

public class ComponentFlowTest {

	public static void main(final String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		if (args.length < 1) {
			System.out.println("Check args");
			return;
		}
		SQLManager s = null;
		try {
			// Init db connection
			s = new SQLManager();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println("Check database is alive and correctly initialised (Schema,User,Password: cflow");
			return;
		}

		if (args[0].equalsIgnoreCase("--add")) {
			// Provide SQLManager
			TestCompat.mgr = s;
			// args[1] is wear or phone
			TestCompat.flowDroid(args);
		} else if (args[0].equalsIgnoreCase("--analyse")) {
			if (args.length == 2) {
				s.analyseFlows(args[1]);
			} else {
				s.analyseFlows(args[1], args[2]);
			}
		} else if (args[0].equalsIgnoreCase("--newsemantic")) {
			s.addSemantic(args[1], args[2], args[3]);
		} else {
			System.out.println("Error: use --add, --analyse, or --newsemantic");
		}

		s.close();
	}

}