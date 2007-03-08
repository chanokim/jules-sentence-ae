/** 
 * SentenceAnnotatorTest.java
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
 * This is a JUnit test for the SentenceAnnotator.
 **/

package de.julielab.jules.ae;

import java.util.Iterator;

import com.ibm.uima.UIMAFramework;
import com.ibm.uima.analysis_engine.AnalysisEngine;
import com.ibm.uima.jcas.JFSIndexRepository;
import com.ibm.uima.jcas.impl.JCas;
import com.ibm.uima.resource.ResourceInitializationException;
import com.ibm.uima.resource.ResourceSpecifier;
import com.ibm.uima.util.XMLInputSource;

import de.julielab.jules.types.Sentence;

import junit.framework.TestCase;

public class SentenceAnnotatorTest extends TestCase {

	private String descriptor = "src/test/resources/SentenceAnnotatorTest.xml";

	private String[] texts = {
			"First sentence. Second \t sentence! \n    Last sentence?",
			"Hallo, jemand da? Nein, niemand.",
			""};

	private String[] offsets = { "0-15;16-34;40-54", "0-17;18-32","" };

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
			e.printStackTrace();
		}

		for (int i = 0; i < texts.length; i++) {

			JCas jcas = null;
			try {
				jcas = sentenceAnnotator.newJCas();
			} catch (ResourceInitializationException e) {
				e.printStackTrace();
			}

			System.out.println("\ntesting text: \n" + texts[i]);
			jcas.setDocumentText(texts[i]);

			try {
				sentenceAnnotator.process(jcas, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get the offsets of the sentences
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
			Iterator sentIter = indexes.getAnnotationIndex(Sentence.type)
					.iterator();

			String predictedOffsets = "";

			while (sentIter.hasNext()) {
				Sentence s = (Sentence) sentIter.next();
				//System.out.println(s.getCoveredText() + ": " + s.getBegin()
				//		+ " - " + s.getEnd());
				predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
				predictedOffsets += s.getBegin() + "-" + s.getEnd();
			}

			System.out.println("\npredicted: " + predictedOffsets);
			System.out.println("wanted: " + offsets[i]);

			// compare offsets
			if (!predictedOffsets.equals(offsets[i])) {
				annotationsOK = false;
				continue;
			}
		}

		assertTrue(annotationsOK);

	}

}
