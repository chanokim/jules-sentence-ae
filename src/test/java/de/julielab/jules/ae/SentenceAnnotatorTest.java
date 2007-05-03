/** 
 * SentenceAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.2 	
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a JUnit test for the SentenceAnnotator.
 **/

package de.julielab.jules.ae;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

import de.julielab.jules.types.Sentence;

import junit.framework.TestCase;

public class SentenceAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(SentenceAnnotatorTest.class);
	
	private String descriptor = "src/test/resources/SentenceAnnotatorTest.xml";

	private String[] texts = {
			"First sentence. Second \t sentence! \n    Last sentence?",
			"Hallo, jemand da? Nein, niemand.", "A test. METHODS: Bad stuff.",
			"" };

	private String[] offsets = { "0-15;16-34;40-54", "0-17;18-32",
			"0-7;8-16;17-27", "" };

	
	protected void setUp() throws Exception {
		super.setUp();
		// set log4j properties file
		PropertyConfigurator.configure("src/test/java/log4j.properties");
	}
	
	public void testProcess() {
	
		boolean annotationsOK = true;

		XMLInputSource sentenceXML = null;
		ResourceSpecifier sentenceSpec = null;
		AnalysisEngine sentenceAnnotator = null;

		try {
			sentenceXML = new XMLInputSource(descriptor);
			sentenceSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					sentenceXML);
			sentenceAnnotator = UIMAFramework
					.produceAnalysisEngine(sentenceSpec);
		} catch (Exception e) {
			LOGGER.error("testProcess()", e); //$NON-NLS-1$
		}

		for (int i = 0; i < texts.length; i++) {

			JCas jcas = null;
			try {
				jcas = sentenceAnnotator.newJCas();
			} catch (ResourceInitializationException e) {
				LOGGER.error("testProcess()", e); //$NON-NLS-1$
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("testProcess() - testing text: " + texts[i]); //$NON-NLS-1$
			}
			jcas.setDocumentText(texts[i]);

			try {
				sentenceAnnotator.process(jcas, null);
			} catch (Exception e) {
				LOGGER.error("testProcess()", e); //$NON-NLS-1$
			}

			// get the offsets of the sentences
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
			Iterator sentIter = indexes.getAnnotationIndex(Sentence.type)
					.iterator();

			String predictedOffsets = "";

			while (sentIter.hasNext()) {
				Sentence s = (Sentence) sentIter.next();
				// System.out.println("sentence: " + s.getCoveredText() + ": " +
				// s.getBegin()
				// + " - " + s.getEnd());
				predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
				predictedOffsets += s.getBegin() + "-" + s.getEnd();
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("testProcess() - predicted: " + predictedOffsets); //$NON-NLS-1$
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("testProcess() - wanted: " + offsets[i]); //$NON-NLS-1$
			}

			// compare offsets
			if (!predictedOffsets.equals(offsets[i])) {
				annotationsOK = false;
				continue;
			}
		}

		assertTrue(annotationsOK);

	}

}
