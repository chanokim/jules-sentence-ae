/** 
 * SentenceAnnotator.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.0 	
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a wrapper to the JULIE Sentence Boundary Detector. It splits a text into single sentences
 * and adds annotations of type Sentence to the respective (J)Cas.
 **/

package de.julielab.jules.ae;

import java.util.ArrayList;

import com.ibm.uima.UimaContext;
import com.ibm.uima.analysis_component.JCasAnnotator_ImplBase;
import com.ibm.uima.jcas.impl.JCas;
import com.ibm.uima.resource.ResourceInitializationException;

import de.julielab.jsbd.JSBDException;
import de.julielab.jsbd.SentenceSplitter;
import de.julielab.jsbd.Unit;
import de.julielab.jules.types.Sentence;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

	private boolean doPostprocessing = false;

	private SentenceSplitter sentenceSplitter;

	/**
	 * initiaziation of JSBD: load the model
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		System.out.println("initializing JSBD...");

		// invoke default initialization
		super.initialize(aContext);

		String modelFilename = "";

		// get parameters
		modelFilename = (String) aContext
				.getConfigParameterValue("ModelFilename");

		// as this parameter is not mandatory, first check whether it is
		// provided...
		Boolean pp = (Boolean) aContext
				.getConfigParameterValue("Postprocessing");
		if (pp != null) {
			doPostprocessing = ((Boolean) aContext
					.getConfigParameterValue("Postprocessing")).booleanValue();
		}

		sentenceSplitter = new SentenceSplitter();
		try {
			sentenceSplitter.readModel(modelFilename);
		} catch (Exception e) {
			System.err.println("Could not load sentence splitter model: "
					+ e.getMessage());
			throw new ResourceInitializationException();
		}

		System.out.println("sentence splitter initialized");
	}

	public void process(JCas aJCas) {

		System.out.println(" JSBD: processing next document...");

		// get the text from the cas
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(aJCas.getDocumentText());

		// make prediction
		ArrayList<Unit> units;
		try {
			units = sentenceSplitter.predict(lines, doPostprocessing);
		} catch (JSBDException e) {
			System.err.println("JSBD error: " + e.getMessage());
			throw new RuntimeException();
			// throw new AnnotatorProcessException();
		}

		// add to UIMA annotations
		addAnnotations(aJCas, units);

	}

	/**
	 * add all the sentences to CAS
	 * @param aJCas the associated JCas
	 * @param units
	 *            all sentence units as returned by the JSBD
	 */
	private void addAnnotations(JCas aJCas, ArrayList<Unit> units) {
		int start = 0;
		for (int i = 0; i < units.size(); i++) {
			Unit myUnit = units.get(i);
			String decision = units.get(i).label;
			if (decision.equals("EOS")) { // end-of-sentence predicted (EOS)
				Sentence annotation = new Sentence(aJCas);
				annotation.setBegin(start);
				annotation.setEnd(myUnit.end);
				annotation.addToIndexes();
				start = -1;
			} else {
				if (start == -1) { // now a new sentence is starting
					start = myUnit.begin;
				}
			}
		}
	}

}
