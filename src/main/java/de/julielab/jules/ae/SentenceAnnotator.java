/** 
 * SentenceAnnotator.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.1.1 	
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a wrapper to the JULIE Sentence Boundary Detector. It splits a text into single sentences
 * and adds annotations of type Sentence to the respective (J)Cas.
 **/

package de.julielab.jules.ae;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.ibm.uima.UimaContext;
import com.ibm.uima.analysis_component.JCasAnnotator_ImplBase;
import com.ibm.uima.jcas.impl.JCas;
import com.ibm.uima.resource.ResourceInitializationException;

import de.julielab.jsbd.JSBDException;
import de.julielab.jsbd.SentenceSplitter;
import de.julielab.jsbd.Unit;
import de.julielab.jules.types.Sentence;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger
			.getLogger(SentenceAnnotator.class);

	private boolean doPostprocessing = false;

	private SentenceSplitter sentenceSplitter;

	
	/**
	 * initiaziation of JSBD: load the model, set post processing
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		logger.info("[JSBD] initializing...");

		// invoke default initialization
		super.initialize(aContext);

		// get parameters
		String modelFilename = (String) aContext
				.getConfigParameterValue("ModelFilename");

		// this parameter is not mandatory, so first check whether it is there
		Boolean pp = (Boolean) aContext
				.getConfigParameterValue("Postprocessing");
		if (pp != null) {
			doPostprocessing = ((Boolean) aContext
					.getConfigParameterValue("Postprocessing")).booleanValue();
		}

		// initialize sentenceSplitter
		sentenceSplitter = new SentenceSplitter();
		try {
			sentenceSplitter.readModel(modelFilename);
		} catch (Exception e) {
			logger.error("[JSBD] Could not load sentence splitter model: "
					+ e.getMessage());
			throw new ResourceInitializationException();
		}
	}

	
	
	public void process(JCas aJCas) {

		logger.info("[JSBD] processing document...");

		// get the text from the cas
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(aJCas.getDocumentText());

		// make prediction
		ArrayList<Unit> units;
		try {
			units = sentenceSplitter.predict(lines, doPostprocessing);
		} catch (JSBDException e) {
			logger.error("[JSBD] " + e.getMessage());
			throw new RuntimeException();
		}

		// add to UIMA annotations
		addAnnotations(aJCas, units);
	}

	
	
	/**
	 * Add all the sentences to CAS. Sentence is split into single units, for
	 * each such unit we decide whether this unit is at the end of a sentence.
	 * If so, this unit gets the label "EOS" (end-of-sentence).
	 * 
	 * @param aJCas
	 *            the associated JCas
	 * @param units
	 *            all sentence units as returned by JSBD
	 */
	private void addAnnotations(JCas aJCas, ArrayList<Unit> units) {
		int start = 0;
		for (int i = 0; i < units.size(); i++) {
			Unit myUnit = units.get(i);
			String decision = units.get(i).label;

			if (start == -1) { // now a new sentence is starting
				start = myUnit.begin;
			}

			if (decision.equals("EOS")) { // end-of-sentence predicted (EOS)
				Sentence annotation = new Sentence(aJCas);
				annotation.setBegin(start);
				annotation.setEnd(myUnit.end);
				annotation.setComponentId("JULIE Sentence Boundary Detector");
				annotation.addToIndexes();
				start = -1;
			}

		}
	}

}
