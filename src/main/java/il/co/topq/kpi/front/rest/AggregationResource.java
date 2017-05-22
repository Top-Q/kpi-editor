package il.co.topq.kpi.front.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import il.co.topq.kpi.StopWatch;
import il.co.topq.kpi.model.ElasticDatabase;
import il.co.topq.kpi.model.ElasticsearchTest;
import il.co.topq.kpi.view.AbstractTableView;
import il.co.topq.kpi.view.DataTable;
import il.co.topq.kpi.view.SwFailureRatesAggsTableView;

@RestController
@Path("api/aggs")
public class AggregationResource {

	private static final Logger log = LoggerFactory.getLogger(AggregationResource.class);

	@Autowired
	private ElasticDatabase db;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/swFailures/{type}/{daysToDate: [0-999]+}")
	public DataTable getSWFailureRatesPerBranch(@PathParam("type") String type, @PathParam("daysToDate") int daysToDate)
			throws IOException {
		log.debug("GET - SW Failure rates per branch");

		final AbstractTableView<ElasticsearchTest> tableView = new SwFailureRatesAggsTableView();
		StopWatch stopWatch = new StopWatch(log).start("Getting from Elastic all tests within the given days");
		List<ElasticsearchTest> tests = db.getTestsByDays(daysToDate);
		stopWatch.stopAndLog();
		if (tests.isEmpty()) {
			return tableView.getTable();
		}

		stopWatch = stopWatch.start("Sorting tests by branch");
		// @formatter:off
		tests = tests.stream()
				.filter(test -> filterByType(test, type))
				.filter(test -> filterByBranch(test))
				.sorted((test0, test1) -> compareByBranch(test0, test1))
				.collect(Collectors.toList());
		// @formatter:on
		stopWatch.stopAndLog();

		stopWatch = stopWatch.start("Populating table");
		tableView.populate(tests);
		stopWatch.stopAndLog();

		return tableView.getTable();
	}

	private boolean filterByType(ElasticsearchTest test, String type) {
		final String testType = test.getScenarioProperties().get("Type");
		if (null == testType) {
			return false;
		}
		return testType.equals(type);
	}

	private boolean filterByBranch(ElasticsearchTest test) {
		final String branch = test.getScenarioProperties().get("Branch");
		if (null == branch) {
			return false;
		}
		return branch.matches("\\d{2}\\.\\d{2}\\.\\d{3}");

	}

	/**
	 * @param test0
	 * @param test1
	 * @return
	 */
	private int compareByBranch(ElasticsearchTest test0, ElasticsearchTest test1) {
		final List<Integer> branch0 = Arrays.asList(test0.getScenarioProperties().get("Branch").split("\\.")).stream()
				.map(Integer::parseInt).collect(Collectors.toList());
		final List<Integer> branch1 = Arrays.asList(test1.getScenarioProperties().get("Branch").split("\\.")).stream()
				.map(Integer::parseInt).collect(Collectors.toList());
		for (int i = 0; i < 3; i++) {
			if (branch0.get(i) != branch1.get(i)) {
				return branch1.get(i) - branch0.get(i);
			}

		}
		return 0;
	}

}
