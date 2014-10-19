/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.choiceSetGeneration.helper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.Time;

public class SpanningTree {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(SpanningTree.class);

	private Node origin = null;
	private double dTime = Time.UNDEFINED_TIME;

	private final TravelTime ttFunction;
	private final TravelDisutility tcFunction;
	private HashMap<Id<Node>,NodeData> nodeData;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public SpanningTree(TravelTime tt, TravelDisutility tc) {
		log.info("init " + this.getClass().getName() + " module...");
		this.ttFunction = tt;
		this.tcFunction = tc;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	public static class NodeData {
		private Node prev = null;
		private double cost = Double.MAX_VALUE;
		private double time = 0;
		public void reset() { this.prev = null; this.cost = Double.MAX_VALUE; this.time = 0; }
		public void visit(final Node comingFrom, final double cost, final double time) {
			this.prev = comingFrom;
			this.cost = cost;
			this.time = time;
		}
		public double getCost() { return this.cost; }
		public double getTime() { return this.time; }
		public Node getPrevNode() { return this.prev; }
	}

	static class ComparatorCost implements Comparator<Node> {
		protected Map<Id<Node>, ? extends NodeData> nodeData;
		ComparatorCost(final Map<Id<Node>, ? extends NodeData> nodeData) { this.nodeData = nodeData; }
		@Override
		public int compare(final Node n1, final Node n2) {
			double c1 = getCost(n1);
			double c2 = getCost(n2);
			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return n1.getId().compareTo(n2.getId());
		}
		protected double getCost(final Node node) { return this.nodeData.get(node.getId()).getCost(); }
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigin(Node origin) {
		this.origin = origin;
	}

	public final void setDepartureTime(double time) {
		this.dTime = time;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final HashMap<Id<Node>,NodeData> getTree() {
		return this.nodeData;
	}

	public final void getNodesByTravelTimeBudget(double travelTimeBudget, List<Node>  nodesList, List<Double> travelTimesList) {
		HashMap<Id<Node>,NodeData> tree = this.nodeData;
		for (Id<Node> id : tree.keySet()) {
			NodeData d = tree.get(id);
			if ((d.getPrevNode() != null) && (d.getCost() <= travelTimeBudget)) {
					// TODO: Check if this is ok 'prev'!
					nodesList.add(d.getPrevNode());
					travelTimesList.add(d.getCost());
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void relaxNode(final Node n, PriorityQueue<Node> pendingNodes) {
		NodeData nData = nodeData.get(n.getId());
		double currTime = nData.getTime();
		double currCost = nData.getCost();
		for (Link l : n.getOutLinks().values()) {
			Node nn = l.getToNode();
			NodeData nnData = nodeData.get(nn.getId());
			if (nnData == null) { nnData = new NodeData(); this.nodeData.put(nn.getId(),nnData); }
			double visitCost = currCost+tcFunction.getLinkTravelDisutility(l,currTime, null, null);
			double visitTime = currTime+ttFunction.getLinkTravelTime(l,currTime, null, null);
			if (visitCost < nnData.getCost()) {
				pendingNodes.remove(nn);
				nnData.visit(n,visitCost,visitTime);
				pendingNodes.add(nn);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Network network) {

		nodeData = new HashMap<>((int)(network.getNodes().size()*1.1),0.95f);
		NodeData d = new NodeData();
		d.time = dTime;
		d.cost = 0;
		nodeData.put(origin.getId(),d);

		ComparatorCost comparator = new ComparatorCost(nodeData);
		PriorityQueue<Node> pendingNodes = new PriorityQueue<Node>(500,comparator);
		relaxNode(this.origin,pendingNodes);
		while (!pendingNodes.isEmpty()) {
			Node n = pendingNodes.poll();
			relaxNode(n,pendingNodes);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile("../../input/network.xml");
		Config conf = scenario.getConfig();
		TravelTime ttc = new TravelTimeCalculator(network,60,30*3600, conf.travelTimeCalculator()).getLinkTravelTimes();
		SpanningTree st = new SpanningTree(ttc,new TravelTimeAndDistanceBasedTravelDisutility(ttc, conf.planCalcScore()));
		Node origin = network.getNodes().get(Id.create(1, Node.class));
		st.setOrigin(origin);
		st.setDepartureTime(8*3600);
		st.run(network);
		HashMap<Id<Node>,NodeData> tree = st.getTree();
		for (Id<Node> id : tree.keySet()) {
			NodeData d = tree.get(id);
			if (d.getPrevNode() != null) {
				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+d.getPrevNode().getId());
			}
			else {
				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+"0");
			}
		}
	}
}
