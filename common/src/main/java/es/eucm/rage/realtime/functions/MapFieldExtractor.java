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
package es.eucm.rage.realtime.functions;

import es.eucm.rage.realtime.topologies.TopologyBuilder;
import org.apache.storm.trident.operation.Function;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.operation.TridentOperationContext;
import org.apache.storm.trident.tuple.TridentTuple;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MapFieldExtractor implements Function {
	private static final Logger LOGGER = Logger
			.getLogger(MapFieldExtractor.class.getName());

	private final String mapKey;
	private final String[] fields;

	/**
	 * Extracts fields from a Map identified by the "mapKey" parameter.
	 * 
	 * @param fields
	 *            used to extracts objects from the {@link Map} and emit them
	 *            preserving the order. May contain "." which will search in
	 *            nested objects of the map.
	 */
	public MapFieldExtractor(String mapKey, String... fields) {
		this.mapKey = mapKey;
		this.fields = fields;
	}

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		try {
			Map trace = (Map) tuple.getValueByField(mapKey);
			ArrayList<Object> object = new ArrayList<Object>();
			for (String field : fields) {
				if (field.contains(".")) {
					String[] nested = field.split(Pattern.quote("."));
					Map nestedMap = trace;
					for (int i = 0; i < nested.length - 1; ++i) {
						nestedMap = (Map) nestedMap.get(nested[i]);
					}
					object.add(nestedMap.get(nested[nested.length - 1]));
				} else {
					object.add(trace.get(field));
				}
			}
			collector.emit(object);
		} catch (Exception ex) {
			LOGGER.info("Error unexpected exception, discarding "
					+ ex.toString());
		}
	}

	@Override
	public void prepare(Map conf, TridentOperationContext context) {

	}

	@Override
	public void cleanup() {

	}
}