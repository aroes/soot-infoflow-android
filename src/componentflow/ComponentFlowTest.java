package componentflow;

import java.io.IOException;

import soot.jimple.infoflow.android.TestApps.TestCompat;

public class ComponentFlowTest {

	public static void main(final String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		if (args.length < 1) {
			System.out.println("Check args");
			return;
		}
		
		if (args[0].equalsIgnoreCase("-add")) {
			//args[1] is wear or phone
			TestCompat.flowDroid(args);
		}
		else if (args[0].equalsIgnoreCase("-analyse")) {
			SQLManager s = new SQLManager();
			s.analyseFlows(args[1], args[2]);
		}
	}

}