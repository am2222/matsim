/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutator.java
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

package org.matsim.locationchoice;

import java.util.Iterator;
import java.util.TreeMap;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.collections.QuadTree;


public abstract class LocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	protected NetworkLayer network = null;
	protected Controler controler = null;	
	protected TreeMap<String, QuadTree<Facility>> quad_trees;
		
//	private static final Logger log = Logger.getLogger(LocationMutator.class);
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler) {
		this.initialize(network, controler);
	}
	
	public LocationMutator(final NetworkLayer network) {
		this.initialize(network, null);
	}
	
	private void initTrees(Facilities facilities) {
		
		TreeMap<String, TreeMap<Id, Facility>> trees = new TreeMap<String, TreeMap<Id, Facility>>();
		
		// get all types of activities
		Iterator<Facility> fac_it = facilities.iterator();
		while (fac_it.hasNext()) {
			Facility f = fac_it.next();
			TreeMap<String, Activity> activities = f.getActivities();
			
			Iterator<Activity> act_it = activities.values().iterator();
			while (act_it.hasNext()) {
				Activity act = act_it.next();
				
				if (!trees.containsKey(act.getType())) {
					trees.put(act.getType(), new TreeMap<Id, Facility>());
				}
				trees.get(act.getType()).put(f.getId(), f);
			}	
		}
		
		// create the quadtrees
		Iterator<TreeMap<Id, Facility>> tree_it = trees.values().iterator();
		Iterator<String> type_it = trees.keySet().iterator();
			
		while (tree_it.hasNext()) {
			TreeMap<Id, Facility> tree_of_type = tree_it.next();
			String type = type_it.next();						
			this.quad_trees.put(type, this.builFacQuadTree(tree_of_type));		
		}	
	}

	private void initialize(final NetworkLayer network, Controler controler) {
			
		//create a quadtree for every activity type
		this.initTrees(controler.getFacilities());
					
		this.network = network;
		this.controler = controler;
	
	}

	public void handlePlan(final Plan plan){
	}


	@Override
	public void run(final Person person) {
		final int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			final Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {	
		handlePlan(plan);
	}
	
	private QuadTree<Facility> builFacQuadTree(TreeMap<Id,Facility> facilities_of_type) {
		Gbl.startMeasurement();
		System.out.println("      building facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final Facility f : facilities_of_type.values()) {
			if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
			if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
			if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
			if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Facility> quadtree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (final Facility f : facilities_of_type.values()) {
			quadtree.put(f.getCenter().getX(),f.getCenter().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
		return quadtree;
	}

	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
}
