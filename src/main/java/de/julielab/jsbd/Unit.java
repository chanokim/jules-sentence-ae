/** 
 * Unit.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The Unit object.
 * 
 **/

package de.julielab.jsbd;


public class Unit {

	public int begin; // start offset
	public int end;  // end offset
	public String rep; // string representation of this unit
	public String label;  // the predicted label
	
	
	public Unit(int begin, int end, String rep) {
		this.begin=begin;
		this.end=end;
		this.rep=rep;
		this.label="O";
	}

	
	public String toString() {
		return rep+": " + begin+"-"+end;
	}
}
