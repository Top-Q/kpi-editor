package il.co.topq.report.front.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import il.co.topq.elastic.ESClient;
import il.co.topq.report.Common;
import il.co.topq.report.Configuration;
import il.co.topq.report.Configuration.ConfigProps;
import il.co.topq.report.business.ElasticsearchTest;

@RestController
@Path("api/execution")
public class ExecutionResource {

	private static final Logger log = LoggerFactory.getLogger(TestResource.class);

	private enum Header {

		// @formatter:off
		EXECUTION("Execution"),
		DATE("Date"),
		TYPE("Type"),
		BRANCH("Branch"),
		BUILD("Build"), 
		SETUP("Setup"),
		TOTAL_QUANTITY("Total Quantity"),
		TOTAL_PASS("Total Pass"),
		TOTAL_FAILURES("Total Failures"),
		SETUP_ISSUES("Setup Issues"),
		AUTOMATION_ISSUES("Automation Issues"),
		SW_ISSUES("SW Issues"),
		UNSPECIFIED_ISSUES("Unspecifed Issues");
		// @formatter:on

		public final String headerName;

		private Header(String headerName) {
			this.headerName = headerName;
		}

	}

	private class ExecutionData {

		final int id;
		final String date;
		final String branch;
		final String setup;
		final String type;
		int totalQuantity = 0;
		int totalPass = 0;
		int totalFaliures = 0;
		int setupIssues = 0;
		int autoIssues = 0;
		int swIssues = 0;
		int unspecIssues = 0;

		private ExecutionData(ElasticsearchTest test) {
			super();
			this.id = test.getExecutionId();
			this.date = test.getExecutionTimeStamp();
			if (test.getScenarioProperties() != null && !test.getScenarioProperties().isEmpty()) {
				branch = test.getScenarioProperties().get("Branch");
				setup = test.getScenarioProperties().get("SetupName");
				type = test.getScenarioProperties().get("Type");
			} else {
				branch = "";
				setup = "";
				type = "";

			}

		}

	}

	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public DataTable get() throws IOException {
		log.debug("GET - Get all the executions within the given time frame");
		Set<String> headers = new LinkedHashSet<>();
		for (Header header : Header.values()) {
			headers.add(header.headerName);
		}
		final DataTable table = new DataTable(headers);
		List<ElasticsearchTest> tests = null;
		try (ESClient client = new ESClient(Configuration.INSTANCE.readString(ConfigProps.ELASTIC_HOST),
				Configuration.INSTANCE.readInt(ConfigProps.ELASTIC_HTTP_PORT))) {

			Map<String, Object> rangeParams = new HashMap<String, Object>();
			rangeParams.put("gte", "now-30d");
			// @formatter:off
			tests = client.index(Common.ELASTIC_INDEX)
					.document(Common.ELASTIC_DOC)
					.search()
					.byRange("executionTimestamp",rangeParams)
					.asClass(ElasticsearchTest.class);
			// @formatter:on
		}
		if (null == tests || tests.isEmpty()) {
			log.warn("No tests were found in the Elastic");
			return table;
		}

		log.debug("Found " + tests.size() + " tests in the given time frame");

		tests = tests.stream().sorted((test0, test1) -> test1.getExecutionId() - test0.getExecutionId())
				.collect(Collectors.toList());

		populateTable(table, tests);
		return table;
	}

	private void populateTable(final DataTable table, List<ElasticsearchTest> tests) {
		ExecutionData eData = new ExecutionData(tests.get(0));

		for (ElasticsearchTest test : tests) {
			if (eData.id != test.getExecutionId()) {
				createTableRow(table, eData);
				eData = new ExecutionData(test);
			}
			eData.totalQuantity++;
			if ("success".equals(test.getStatus())) {
				eData.totalPass++;
				continue;
			}
			eData.totalFaliures++;
			if (test.getProperties() == null || test.getProperties().get("issueType") == null) {
				eData.unspecIssues++;
				continue;
			}
			switch (test.getProperties().get("issueType")) {
			case "SW":
				eData.swIssues++;
				break;
			case "Auto":
				eData.autoIssues++;
				break;
			case "Setup":
				eData.setupIssues++;
				break;
			default:
				eData.unspecIssues++;
				break;
			}
		}
		createTableRow(table, eData);
	}

	private void createTableRow(final DataTable table, ExecutionData eData) {
		log.debug("Execution " + eData.id + " found with " + eData.totalQuantity + " tests");
		Map<String, Object> row = new HashMap<String, Object>();
		row.put(Header.EXECUTION.headerName, eData.id);
		row.put(Header.DATE.headerName, eData.date);
		row.put(Header.TYPE.headerName, eData.type);
		row.put(Header.BRANCH.headerName, eData.branch);
		row.put(Header.BUILD.headerName, "");
		row.put(Header.SETUP.headerName, eData.setup);
		row.put(Header.TOTAL_QUANTITY.headerName, eData.totalQuantity);
		row.put(Header.TOTAL_PASS.headerName, eData.totalPass);
		row.put(Header.TOTAL_FAILURES.headerName, eData.totalFaliures);
		row.put(Header.SETUP_ISSUES.headerName, eData.setupIssues);
		row.put(Header.AUTOMATION_ISSUES.headerName, eData.autoIssues);
		row.put(Header.SW_ISSUES.headerName, eData.swIssues);
		row.put(Header.UNSPECIFIED_ISSUES.headerName, eData.unspecIssues);
		table.addRow(row);
	}

}
