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
package es.eucm.rage.realtime.simple.filters;

import es.eucm.rage.realtime.topologies.TopologyBuilder;
import org.apache.storm.trident.operation.Filter;
import org.apache.storm.trident.operation.TridentOperationContext;
import org.apache.storm.trident.tuple.TridentTuple;

import java.util.Map;
import java.util.logging.Logger;

public class IsBubbledTrace implements Filter {
	private static final Logger LOGGER = Logger.getLogger(IsBubbledTrace.class
			.getName());
	public static final boolean LOG = true;

	private String traceKey;

	/**
	 * Filters a Trace TridentTuple to see if it has been bubbled at least once.
	 * If it has been bubbled the {@link TopologyBuilder#CHILD_ACTIVITY_ID_KEY}
	 * must not be null and be different than
	 * {@link TopologyBuilder#ACTIVITY_ID_KEY}. (if they are equal it means that
	 * the trace has been sent to the same node, not upwards to the parent node)
	 * 
	 */
	public IsBubbledTrace(String traceKey) {
		this.traceKey = traceKey;
	}

	@Override
	public boolean isKeep(TridentTuple objects) {
		try {
			Object traceObject = objects.getValueByField(traceKey);

			if (!(traceObject instanceof Map)) {
				if (LOG) {
					LOGGER.info(traceKey + " field of tuple " + objects
							+ " is not a map, found: " + traceObject);
				}
				return false;
			}

			Map traceMap = (Map) traceObject;

			Object childActivityObject = traceMap
					.get(TopologyBuilder.CHILD_ACTIVITY_ID_KEY);

			if (childActivityObject != null) {
				// If has been bubbled it will have a CHILD_ACTIVITY_ID_KEY
				// AND the CHILD_ACTIVITY_ID_KEY will be different
				// than the current ACTIVITY_ID_KEY
				return !childActivityObject.equals(traceMap
						.get(TopologyBuilder.ACTIVITY_ID_KEY));
			}

			return false;
		} catch (Exception ex) {
			LOGGER.info("Error unexpected exception, discarding"
					+ ex.toString());
			return false;
		}
	}

	@Override
	public void prepare(Map map, TridentOperationContext tridentOperationContext) {

	}

	@Override
	public void cleanup() {

	}
}
