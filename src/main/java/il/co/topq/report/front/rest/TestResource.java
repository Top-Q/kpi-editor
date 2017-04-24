package il.co.topq.report.front.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import il.co.topq.report.Common;
import il.co.topq.report.Configuration;
import il.co.topq.report.Configuration.ConfigProps;
import il.co.topq.report.business.ElasticsearchTest;

@RestController
@Path("api/execution/{executionId}/test")
public class TestResource {

	private static final Logger log = LoggerFactory.getLogger(TestResource.class);

	private enum Header {
		EXECUTION("Execution"),UID("Uid"), NAME("Name"), DESCRIPTION("Description"), STATUS("Status"), FAILURE_REASON(
				"Failure Reason"), ISSUE_TYPE("Issue Type");

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
		for (ElasticsearchTest test : tests) {
			// if (!"success".equals(test.getStatus())){
			Map<String, Object> row = new HashMap<String, Object>();
			row.put(Header.EXECUTION.headerName, test.getExecutionId()	);
			row.put(Header.UID.headerName, test.getUid());
			row.put(Header.NAME.headerName, test.getName());
			row.put(Header.DESCRIPTION.headerName, test.getDescription());
			row.put(Header.STATUS.headerName, test.getStatus());
			row.put(Header.FAILURE_REASON.headerName, "");
			row.put(Header.ISSUE_TYPE.headerName, "");
			table.addRow(row);

			// }
		}
		return table;
	}

	@PUT
	@Path("/{uid}")
	public void put(@PathParam("executionId") int executionId, @PathParam("uid") String uid,
			@QueryParam("issue") String issue) {
		log.debug("PUT - Changing test with execution id " + executionId + " and uid " + uid + " to issue " + issue);

	}

}
