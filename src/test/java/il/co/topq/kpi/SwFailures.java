package il.co.topq.kpi;

import java.io.IOException;

import il.co.topq.kpi.view.DataTable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SwFailures extends AbstractResource {

	public SwFailures(OkHttpClient client, String baseUrl) {
		super(client, baseUrl);
	}

	public DataTable get(String type, int daysToDate) throws IOException {
		// @formatter:off
		Request request = new Request.Builder()
		  .url(baseUrl + "aggs/swFailures/" + type + "/" + daysToDate)
		  .get()
		  .addHeader("content-type", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .build();
		// @formatter:on

		Response response = client.newCall(request).execute();
		final String responseBody = response.body().string();
		return mapper.readValue(responseBody, DataTable.class);

	}

}
