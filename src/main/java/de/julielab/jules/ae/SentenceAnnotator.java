/** 
 * SentenceAnnotator.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.3
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a wrapper to the JULIE Sentence Boundary Detector (JSBD). 
 * It splits a text into single sentences and adds annotations of 
 * the type Sentence to the respective UIMA (J)Cas.
 **/

package de.julielab.jules.ae;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jsbd.SentenceSplitter;
import de.julielab.jsbd.Unit;
import de.julielab.jules.types.Annotation;
import de.julielab.jules.types.Sentence;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(SentenceAnnotator.class);

	// activate post processing
	private boolean doPostprocessing = false;

	private SentenceSplitter sentenceSplitter;

	private String processingScope;

	/**
	 * initiaziation of JSBD: load the model, set post processing
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("[JSBD] initializing...");

		// invoke default initialization
		super.initialize(aContext);

		// get parameters
		String modelFilename = (String) aContext.getConfigParameterValue("ModelFilename");

		// this parameter is not mandatory, so first check whether it is there
		Boolean pp = (Boolean) aContext.getConfigParameterValue("Postprocessing");
		if (pp != null) {
			doPostprocessing = ((Boolean) aContext.getConfigParameterValue("Postprocessing")).booleanValue();
		}

		// this parameter is not mandatory, so first check whether it is there
		Object obj = aContext.getConfigParameterValue("ProcessingScope");
		if (obj != null) {
			processingScope = (String) aContext.getConfigParameterValue("ProcessingScope");
			processingScope = processingScope.trim();
			if (processingScope.length() == 0)
				processingScope = null;
		} else {
			processingScope = null;
		}
		LOGGER.info("initialize() - processing scope set to: "
						+ ((processingScope == null) ? "document text" : processingScope));
		
		// initialize sentenceSplitter
		sentenceSplitter = new SentenceSplitter();
		try {
			sentenceSplitter.readModel(modelFilename);
		} catch (Exception e) {
			LOGGER.error("[JSBD] Could not load sentence splitter model: " + e.getMessage());
			throw new ResourceInitializationException();
		}
	}

	/**
	 * process method is in charge of doing the sentence splitting. If processingScope is set, we
	 * iterate over Annotation objects of this type and do the sentence splitting within this scope.
	 * Otherwise, the whole document text is considered.
	 * 
	 * @throws AnalysisEngineProcessException
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		LOGGER.info("[JSBD] processing document...");

		// get the text from the cas
		if (processingScope != null) {
			// loop of annotation object of the scope
			try {
				Annotation anno = getAnnotationByClassName(aJCas, processingScope);
				anno.getTypeIndexID();
				JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
				Iterator<org.apache.uima.jcas.tcas.Annotation> iter = indexes.getAnnotationIndex(anno.getTypeIndexID()).iterator();
				while (iter.hasNext()) {
					org.apache.uima.jcas.tcas.Annotation annotation = iter.next();
					String annoText = annotation.getCoveredText();
					if (annoText != null && annoText.length() > 0) {
						doSegmentation(aJCas, annoText);
					}
				}
			} catch (Exception e) {
				LOGGER.error("process() - could not create Annotation object for the ProcessingScope.");
				throw new AnalysisEngineProcessException();
			}
		} else {
			// if no processingScope set -> use documentText
			if (aJCas.getDocumentText() != null && aJCas.getDocumentText().length() > 0) {
				doSegmentation(aJCas, aJCas.getDocumentText());
			} else {
				LOGGER.warn("process() - document text empty. Skipping this document!");
			}
		}
	}

	private void doSegmentation(JCas aJCas, String text) throws AnalysisEngineProcessException {
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(text);

		// make prediction
		ArrayList<Unit> units;
		units = sentenceSplitter.predict(lines, doPostprocessing);

		// add to UIMA annotations
		addAnnotations(aJCas, units);
	}

	/**
	 * Add all the sentences to CAS. Sentence is split into single units, for each such unit we
	 * decide whether this unit is at the end of a sentence. If so, this unit gets the label "EOS"
	 * (end-of-sentence).
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

			if (decision.equals("EOS") || (i == units.size() - 1)) {
				// end-of-sentence predicted (EOS)
				// or last unit reached (finish a last sentence here!)
				Sentence annotation = new Sentence(aJCas);
				annotation.setBegin(start);
				annotation.setEnd(myUnit.end);
				annotation.setComponentId(this.getClass().getName());
				annotation.addToIndexes();
				start = -1;
			}

		}
	}

	/**
	 * returns an annotation object (de.julielab.jules.types.annotation) of the type specified by
	 * fullEntityClassName. This is done by means of dynamic class loading and reflection.
	 * 
	 * @param aJCas
	 *            the jcas to which to link this annotation object
	 * @param fullAnnotationClassName
	 *            the full class name of the new annotation object
	 * @return
	 */
	public static Annotation getAnnotationByClassName(JCas aJCas, String fullAnnotationClassName)
					throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
					InstantiationException, IllegalAccessException, InvocationTargetException {

		Class[] parameterTypes = new Class[] { JCas.class };
		Class myNewClass = Class.forName(fullAnnotationClassName);
		Constructor myConstructor = myNewClass.getConstructor(parameterTypes);
		Annotation anno = (Annotation) myConstructor.newInstance(aJCas);
		return anno;
	}

}
