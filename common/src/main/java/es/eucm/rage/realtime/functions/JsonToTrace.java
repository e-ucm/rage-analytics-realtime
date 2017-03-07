/**
 * Copyright (C) 2016 e-UCM (http://www.e-ucm.es/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.eucm.rage.realtime.functions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.trident.operation.Function;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.operation.TridentOperationContext;
import org.apache.storm.trident.tuple.TridentTuple;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

public class JsonToTrace implements Function {

	private String sessionId;

	private Gson gson;

	private Type type;

	/**
	 * Given a JSON Trace ({@link StringScheme#STRING_SCHEME_KEY} key, from
	 * Kafka) string returns a
	 * {@link es.eucm.rage.realtime.topologies.TopologyBuilder#TRACE_KEY} ->
	 * Map<String, Object>
	 * 
	 * @param sessionId
	 */
	public JsonToTrace(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		Object trace = gson.fromJson(
				tuple.getStringByField(StringScheme.STRING_SCHEME_KEY), type);
		collector.emit(Arrays.asList(sessionId, trace));
	}

	@Override
	public void prepare(Map conf, TridentOperationContext context) {
		gson = new Gson();
		type = new TypeToken<Map<String, Object>>() {
		}.getType();
	}

	@Override
	public void cleanup() {

	}
}
