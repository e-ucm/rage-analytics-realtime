/**
 * Copyright © 2016 e-UCM (http://www.e-ucm.es/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.eucm.rage.realtime;

import com.google.gson.Gson;
import es.eucm.rage.realtime.simple.topologies.TopologyBuilder;
import es.eucm.rage.realtime.utils.CSVToMapTrace;
import es.eucm.rage.realtime.states.elasticsearch.EsMapState;
import es.eucm.rage.realtime.states.elasticsearch.EsState;
import es.eucm.rage.realtime.utils.ESUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.state.StateFactory;
import org.apache.storm.trident.testing.FeederBatchSpout;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link TopologyBuilder} executed on a local cluster and receiving
 * data from a local files TRACES_FILES
 */
public class RealtimeTopologyTest {

	private static final String[] TRACES_FILES = { "DSIR", "KXIH", "SSYP",
			"TQBG", "ZEHU" };
	private static final String NOW_DATE = "name-"
			+ String.valueOf(new Date().getTime());
	private static final String ES_HOST = "localhost";
	private static final String ZOOKEEPER_URL = "localhost";

	@Test
	public void test() throws IOException {

		// Local SPOUT to FEED traces to the local Storm Cluster
		FeederBatchSpout tracesSpout = new FeederBatchSpout(
				Arrays.asList(es.eucm.rage.realtime.topologies.TopologyBuilder.TRACE_KEY));

		TridentTopology topology = new TridentTopology();

		EsState.Factory partitionPersist = new EsState.Factory();
		StateFactory persistentAggregateFactory = new EsMapState.Factory();

		// Test topology Builder configuration
		new TopologyBuilder().build(topology, null,
				topology.newStream("testFileStream", tracesSpout),
				partitionPersist, persistentAggregateFactory, null);

		// Config Object that must be given to the Topology Initialization
		Config conf = new Config();
		conf.put(AbstractAnalysis.ZOOKEEPER_URL_FLUX_PARAM, ZOOKEEPER_URL);
		conf.put(AbstractAnalysis.ELASTICSEARCH_URL_FLUX_PARAM, ES_HOST);
		conf.put(AbstractAnalysis.TOPIC_NAME_FLUX_PARAM, NOW_DATE);
		// Instantiate the Topology in memory (local)
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("realtime", conf, topology.build());

		// Parser that receives a local file in CSV and returns a list of Map
		// Traces
		CSVToMapTrace parser = new CSVToMapTrace();
		String firstIndex = "1-" + NOW_DATE;
		String secondIndex = "2-" + NOW_DATE;
		String className = "class_name-" + NOW_DATE;
		Map<String, Integer> res = new HashMap<>();
		// Read the TEST files, parse them and Feed them to the Local Cluster
		for (int i = 0; i < TRACES_FILES.length; ++i) {
			String idx;
			if (i < 3) {
				idx = firstIndex;
			} else {
				idx = secondIndex;
			}
			List tuples = parser.getTuples(
					"traces/" + TRACES_FILES[i] + ".csv", idx, i, null,
					className);
			tracesSpout.feed(tuples);

			Integer current = res.get(idx);
			if (current == null) {
				res.put(idx, tuples.size());
			} else {
				res.put(idx, current + tuples.size());
			}
		}

		Gson gson = new Gson();

		try {
			// Wait for thr traces to be analyzed by the Topology
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Start querying ElasticSearch and compare the results with the
		// expected values
		RestClient client = RestClient.builder(new HttpHost(ES_HOST, 9200))
				.build();

		int totalClass = 0;
		for (Map.Entry<String, Integer> entry : res.entrySet()) {

			Response response = client.performRequest("GET",
					"/" + entry.getKey() + "/_search?size=5000&q=*:*");
			int status = response.getStatusLine().getStatusCode();

			assertEquals("TEST GET error, status is" + status, status,
					HttpStatus.SC_OK);

			String responseString = EntityUtils.toString(response.getEntity());
			Map<String, Object> responseDocs = (Map) gson.fromJson(
					responseString, Map.class);

			Map hits = (Map) responseDocs.get("hits");

			int total = ((Double) hits.get("total")).intValue();
			totalClass += total;
			assertEquals("Total traces " + entry.getValue() + ", current "
					+ total, Math.abs(total - entry.getValue().intValue()) < 5,
					true);
		}

		// Check total class traces
		Response response = client.performRequest("GET", "/" + className
				+ "/_search?size=5000&q=*:*");
		int status = response.getStatusLine().getStatusCode();

		assertEquals("TEST GET error, status is" + status, status,
				HttpStatus.SC_OK);

		String responseString = EntityUtils.toString(response.getEntity());
		Map<String, Object> responseDocs = (Map) gson.fromJson(responseString,
				Map.class);

		Map hits = (Map) responseDocs.get("hits");

		int totalClassDB = ((Double) hits.get("total")).intValue();
		assertEquals("Total traces " + className + ", current " + totalClassDB,
				totalClassDB, totalClass);

		// Parse the results folder of the verbs and compare them
		for (int i = 0; i < TRACES_FILES.length; ++i) {
			List<String> lines = parser.getLines("traces/results/"
					+ TRACES_FILES[i] + ".csv-result");

			String idx;
			if (i < 3) {
				idx = firstIndex;
			} else {
				idx = secondIndex;
			}

			String resultsIndex = ESUtils.getResultsIndex(idx);

			Response resultResponse = client.performRequest("GET", "/"
					+ resultsIndex + "/" + ESUtils.getResultsType() + "/" + idx
					+ "_" + TRACES_FILES[i] + ";");
			int resultStatus = resultResponse.getStatusLine().getStatusCode();
			assertEquals("TEST GET result error, status is" + resultStatus,
					resultStatus, HttpStatus.SC_OK);

			String responseResultString = EntityUtils.toString(resultResponse
					.getEntity());
			Map<String, Object> playerState = (Map) gson.fromJson(
					responseResultString, Map.class).get("_source");
			System.out.println("playerState = " + playerState);
			for (String line : lines) {
				String[] keyValue = line.split("=");
				String flatObjectKey = keyValue[0];
				String[] keys = flatObjectKey.split("\\.");

				Map propertyMap = playerState;
				for (int j = 0; j < keys.length - 1; ++j) {
					propertyMap = (Map) propertyMap.get(keys[j]);
				}
				Object value = propertyMap.get(keys[keys.length - 1]);

				if (flatObjectKey
						.startsWith(es.eucm.rage.realtime.topologies.TopologyBuilder.TraceEventTypes.PROGRESSED)
						|| flatObjectKey
								.startsWith(es.eucm.rage.realtime.topologies.TopologyBuilder.TraceEventTypes.COMPLETED)) {

					assertEquals(flatObjectKey, value, keyValue[1]);
				} else {
					assertEquals(flatObjectKey, value,
							Double.valueOf(keyValue[1]));
				}
			}

		}

	}

}
