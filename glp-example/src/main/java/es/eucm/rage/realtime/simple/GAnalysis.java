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
package es.eucm.rage.realtime.simple;

import es.eucm.rage.realtime.AbstractAnalysis;
import es.eucm.rage.realtime.simple.topologies.GLPTopologyBuilder;

import java.util.Map;

/**
 * Main RAGE Analytics {@link AbstractAnalysis} implementation that builds a
 * {@link GLPTopologyBuilder} with the analysis configuration from
 * {@link org.apache.storm.kafka.trident.OpaqueTridentKafkaSpout}. Invoked by
 * Flux using the method {@link AbstractAnalysis#getTopology(Map)}. See:
 * https://
 * github.com/e-ucm/rage-analytics/wiki/Understanding-RAGE-Analytics-Traces
 * -Flow#storm-flux-configuration-files
 */
public class GAnalysis extends AbstractAnalysis {

	private GLPTopologyBuilder topologyBuilder = new GLPTopologyBuilder();

	@Override
	protected es.eucm.rage.realtime.topologies.TopologyBuilder getTopologyBuilder() {
		return topologyBuilder;
	}

}
