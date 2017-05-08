package il.co.topq.report.front.rest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import il.co.topq.elastic.ESClient;
import il.co.topq.elastic.endpoint.Document;
import il.co.topq.report.Common;
import il.co.topq.report.Configuration;
import il.co.topq.report.Configuration.ConfigProps;
import il.co.topq.report.business.ElasticsearchTest;

@RestController
@Path("api/execution/{executionId}/test")
public class TestResource {

	public static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private static final Logger log = LoggerFactory.getLogger(TestResource.class);

	private enum Header {

		// @formatter:off
		EXECUTION("Execution"),
		UID("Uid"),
		NAME("Name"),
		DESCRIPTION("Description"),
		STATUS("Status"),
		FAILURE_REASON("Failure Reason"),
		ISSUE_TYPE("Issue Type");
		// @formatter:on

		public final String headerName;

		private Header(String headerName) {
			this.headerName = headerName;
		}

	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public DataTable get(@PathParam("executionId") int executionId) throws IOException {
		log.debug("GET - Get list of all the tests in the current execution");
		List<ElasticsearchTest> tests = null;
		try (ESClient client = new ESClient(Configuration.INSTANCE.readString(ConfigProps.ELASTIC_HOST),
				Configuration.INSTANCE.readInt(ConfigProps.ELASTIC_HTTP_PORT))) {
			tests = client.index(Common.ELASTIC_INDEX).document(Common.ELASTIC_DOC).search()
					.byTerm("executionId", executionId + "").asClass(ElasticsearchTest.class);
		}
		Set<String> headers = new LinkedHashSet<String>();
		for (Header header : Header.values()) {
			headers.add(header.headerName);
		}
		final DataTable table = new DataTable(headers);
		if (tests.isEmpty()) {
			log.warn("No tests were found in execution with id " + executionId);
			return table;
		}
		// @formatter:off
		tests = tests
				.stream()
				.filter(test -> !"success".equals(test.getStatus()))
				.sorted((test0, test1) -> compareUid(test0, test1))
				.collect(Collectors.toList());
		// @formatter:off

		for (ElasticsearchTest test : tests) {
				Map<String, Object> row = new HashMap<String, Object>();
				row.put(Header.EXECUTION.headerName, test.getExecutionId());
				row.put(Header.UID.headerName, test.getUid());
				row.put(Header.NAME.headerName, test.getName());
				row.put(Header.DESCRIPTION.headerName, test.getDescription());
				row.put(Header.STATUS.headerName, test.getStatus());
				row.put(Header.FAILURE_REASON.headerName, test.getProperties().get("failureReason") == null ? ""
						: test.getProperties().get("failureReason"));
				row.put(Header.ISSUE_TYPE.headerName,
						test.getProperties().get("issueType") == null ? "" : test.getProperties().get("issueType"));
				table.addRow(row);

		}
		return table;
	}

	@PUT
	@Path("/{uid}")
	public void put(@PathParam("executionId") int executionId, @PathParam("uid") String uid,
			@QueryParam("issue") String issue) {
		log.debug("PUT - Changing test with execution id " + executionId + " and uid " + uid + " to issue " + issue);
		try (ESClient client = new ESClient(Configuration.INSTANCE.readString(ConfigProps.ELASTIC_HOST),
				Configuration.INSTANCE.readInt(ConfigProps.ELASTIC_HTTP_PORT))) {
			final Document document = client.index(Common.ELASTIC_INDEX).document(Common.ELASTIC_DOC);

			List<ElasticsearchTest> tests = document.search().byTerm("uid", uid).asClass(ElasticsearchTest.class);
			if (tests.isEmpty()) {
				log.error("No test with " + uid + " was found. Aborting operation");
				return;
			}

			if (tests.size() > 1) {
				log.error("Unexpected tests number with uid " + uid + ". Found " + tests.size()
						+ " tests. Updating only the first one");
			}
			ElasticsearchTest test = tests.get(0);
			log.debug("Updating test with uid " + uid);
			Map<String, String> properties = test.getProperties();
			if (null == properties) {
				properties = new HashMap<String, String>();
			}
			properties.put("issueType", issue);
			test.setProperties(properties);
			document.update().single(uid, test);
			log.debug("Finished updating issue type " + issue + " to test with uid " + uid);

		} catch (IOException e) {
			log.error("Failure due to " + e.getMessage() + " while trying to find or update test with uid " + uid);
		}

	}
	
	private static int compareUid(ElasticsearchTest test0, ElasticsearchTest test1) {
		int test0Id = Integer.parseInt(test0.getUid().substring(test0.getUid().indexOf("-")));
		int test1Id = Integer.parseInt(test1.getUid().substring(test1.getUid().indexOf("-")));
		return test1Id - test0Id;
	}


}
