package il.co.topq.kpi;

import static org.junit.Assert.assertEquals;

import java.util.stream.Collectors;

import org.junit.Test;

import il.co.topq.kpi.view.DataTable;
import il.co.topq.kpi.view.SwFailureRatesAggsTableView;

public class TestAggregationBySwFailureRate extends AbstractResourceTestCase {

	@Test
	public void testRatesPerTypes() throws Exception {
		String branch0 = "12.13.121";

		addTests(10, "sanity", branch0, "falure", SW_ISSUE);
		addTests(90, "sanity", branch0, "success", "");
		
		addTests(20, "regression", branch0, "falure", SW_ISSUE);
		addTests(80, "regression", branch0, "success", "");
		
		addTests(30, "progression", branch0, "falure", SW_ISSUE);
		addTests(70, "progression", branch0, "success", "");

		Thread.sleep(1000);

		DataTable table = client.aggregation().swFailures().get("sanity", 1);
		assertEquals("0.1%", extractSwFailureRate(table, branch0));

		table = client.aggregation().swFailures().get("regression", 1);
		assertEquals("0.2%", extractSwFailureRate(table, branch0));

		table = client.aggregation().swFailures().get("progression", 1);
		assertEquals("0.3%", extractSwFailureRate(table, branch0));

	}

	@Test
	public void testRatesPerDifferentIssues() throws Exception {
		String type = "sanity";
		String branch0 = "12.13.121";

		addTests(10, type, branch0, "falure", SW_ISSUE);
		addTests(10, type, branch0, "failure", SETUP_ISSUE);
		addTests(10, type, branch0, "failure", AUTO_ISSUE);
		addTests(10, type, branch0, "success", "");
		addTests(10, type, branch0, "warning", "");

		Thread.sleep(1000);

		final DataTable table = client.aggregation().swFailures().get("sanity", 1);
		assertEquals("0.2%", extractSwFailureRate(table, branch0));
	}

	@Test
	public void testRatesPerBranches() throws Exception {
		String type = "sanity";
		String branch0 = "12.13.121";
		String branch1 = "12.13.121";
		String branch2 = "12.13.121";

		addTests(10, type, branch0, "falure", SW_ISSUE);
		addTests(10, type, branch0, "success", "");

		addTests(10, type, branch1, "falure", SW_ISSUE);
		addTests(10, type, branch1, "success", "");

		addTests(10, type, branch2, "falure", SW_ISSUE);
		addTests(10, type, branch2, "success", "");

		System.out.println("Going to sleep for one second");
		Thread.sleep(1000);

		final DataTable table = client.aggregation().swFailures().get("sanity", 1);
		assertEquals("0.5%", extractSwFailureRate(table, branch0));
		assertEquals("0.5%", extractSwFailureRate(table, branch1));
		assertEquals("0.5%", extractSwFailureRate(table, branch2));

	}

	private static String extractSwFailureRate(DataTable table, String branch) {
		return table.getData().stream().filter(m -> m.get("Branch").equals(branch)).collect(Collectors.toList()).get(0)
				.get(SwFailureRatesAggsTableView.Header.SW_FAILURE_RATE.getHeaderName()).toString();
	}

}
