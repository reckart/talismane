///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.terminology;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import static org.junit.Assert.*;

import com.joliciel.lefff.LefffService;
import com.joliciel.lefff.LefffServiceLocator;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.terminology.TermExtractorImpl;
import com.joliciel.talismane.terminology.TerminologyBase;
import com.joliciel.talismane.terminology.TermExtractorImpl.Expansion;

public class TermExtractorTest {
	private static final Log LOG = LogFactory.getLog(TermExtractorTest.class);

	@Test
	public void testGetExpansionStrings(@NonStrict final TerminologyBase terminologyBase) throws Exception {
		InputStream tagsetInputStream = getClass().getResourceAsStream("talismaneTagset.txt"); 
		Scanner tagsetScanner = new Scanner(tagsetInputStream);

		InputStream configurationInputStream = getClass().getResourceAsStream("termTestCONLL.txt"); 
		Reader configurationReader = new BufferedReader(new InputStreamReader(configurationInputStream, "UTF-8"));
		
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		PosTaggerService posTaggerService = locator.getPosTaggerServiceLocator().getPosTaggerService();
		PosTagSet tagSet = posTaggerService.getPosTagSet(tagsetScanner);
		TalismaneSession.setPosTagSet(tagSet);
		
		ParserService parserService = locator.getParserServiceLocator().getParserService();
		TransitionSystem transitionSystem = parserService.getArcEagerTransitionSystem();
		TalismaneSession.setTransitionSystem(transitionSystem);
		
		LefffServiceLocator lefffLocator = new LefffServiceLocator();
		LefffService lefffService = lefffLocator.getLefffService();
		LexicalEntryMorphologyReader lefffMorphologyReader = lefffService.getLexicalEntryMorphologyReader();
		
		LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(lefffMorphologyReader);
		
		ParserAnnotatedCorpusReader corpusReader = parserService.getRegexBasedCorpusReader(configurationReader);
		corpusReader.setLexicalEntryReader(lexicalEntryReader);
		
		ParseConfiguration configuration = corpusReader.nextConfiguration();
		LOG.debug(configuration.toString());
		
		new NonStrictExpectations() {
			Term term;
			{
				terminologyBase.getTerm(anyString); returns(term);
			}
		};
		
		TermExtractorImpl termExtractor = new TermExtractorImpl(terminologyBase);
		
		PosTaggedToken chat = configuration.getPosTagSequence().get(3);
		assertEquals("chat", chat.getToken().getText());
		
		// test depth (1)
		int depth = 1;
		Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
		termExtractor.setMaxDepth(depth);
		List<Expansion> expansions = termExtractor.getExpansions(chat, configuration, 0, expansionsPerNoun);
		Set<String> expansionStrings = new TreeSet<String>();
		for (Expansion expansion : expansions) {
			expansionStrings.add(expansion.display());
		}
		
		LOG.debug("All expansions depth " + depth + ":");
		for (String expansionString : expansionStrings){
			LOG.debug(expansionString);
		}
		String[] limitedDepthExpansionArray = new String[] {
				"chat"
		};
		List<String> limitedDepthExpansions = new ArrayList<String>();
		for (String expansion : limitedDepthExpansionArray)
			limitedDepthExpansions.add(expansion);
		
		for (String expansion : limitedDepthExpansions) {
			assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
		}
		assertEquals(limitedDepthExpansions.size(), expansionStrings.size());
		
		// test depth (2)
		depth = 2;
		expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
		termExtractor.setMaxDepth(depth);
		expansions = termExtractor.getExpansions(chat, configuration, 0, expansionsPerNoun);
		expansionStrings = new TreeSet<String>();
		for (Expansion expansion : expansions) {
			expansionStrings.add(expansion.display());
		}

		String[] depth2Array = new String[] {
				"petit chat",
				"chat noir",
				"petit chat noir",
				"chat noir et blanc",
				"petit chat noir et blanc",
				"chat noir et blanc de la grand-mère",
				"petit chat noir et blanc de la grand-mère"
		};
		
		for (String expansion : depth2Array)
			limitedDepthExpansions.add(expansion);

		LOG.debug("Expected expansions depth " + depth + ":");
		for (String expansionString : limitedDepthExpansions){
			LOG.debug(expansionString);
		}
		
		LOG.debug("Actual expansions depth " + depth + ":");
		for (String expansionString : expansionStrings){
			LOG.debug(expansionString);
		}
		
		for (String expansion : limitedDepthExpansions) {
			assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
		}
		
		LOG.debug("Parents and children depth " + depth);
		boolean foundParent1 = false;
		boolean foundParent2 = false;
		boolean foundChild1 = false;
		for (Expansion expansion : expansions) {
			LOG.debug("# " + expansion.display());
			for (Expansion parent : expansion.getParents()) {
				LOG.debug("Parent: " + parent.display());
			}
			for (Expansion child : expansion.getChildren()) {
				LOG.debug("Child: " + child.display());
			}
			
			if (expansion.display().equals("petit chat noir et blanc de la grand-mère")) {
				for (Expansion parent : expansion.getParents()) {
					if (parent.display().equals("chat noir et blanc de la grand-mère")) {
						foundParent1 = true;
					} else if (parent.display().equals("petit chat noir et blanc")) {
						foundParent2 = true;
					}
				}
				for (Expansion child : expansion.getChildren()) {
					if (child.display().equals("grand-mère")) {
						foundChild1 = true;
					}
				}
			}
		}
		assertTrue("Didn't find parent1", foundParent1);
		assertTrue("Didn't find parent2", foundParent2);
		assertTrue("Didn't find child1", foundChild1);
		
		assertEquals(limitedDepthExpansions.size(), expansionStrings.size());
		
		// depth test (3)
		depth = 3;
		String[] depth3Additions = new String[] {
				" maternelle",
		};
		
		List<String> depth3Expansions = new ArrayList<String>();
		for (String depth2Expansion : depth2Array) {
			if (depth2Expansion.endsWith("grand-mère")) {
				for (String depth3Addition : depth3Additions) {
					String newExpansion = depth2Expansion + depth3Addition;
					depth3Expansions.add(newExpansion);
				}
			}
		}
		limitedDepthExpansions.addAll(depth3Expansions);
		LOG.debug("Expected expansions depth " + depth + ":");
		for (String expansionString : limitedDepthExpansions){
			LOG.debug(expansionString);
		}

		expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
		termExtractor.setMaxDepth(depth);
		expansions = termExtractor.getExpansions(chat, configuration, 0, expansionsPerNoun);
		expansionStrings = new TreeSet<String>();
		for (Expansion expansion : expansions) {
			expansionStrings.add(expansion.display());
		}
		LOG.debug("Actual expansions depth " + depth + ":");
		for (String expansionString : expansionStrings){
			LOG.debug(expansionString);
		}
		
		for (String expansion : limitedDepthExpansions) {
			assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
		}
		
		LOG.debug("Parents and children depth " + depth);
		for (Expansion expansion : expansions) {
			LOG.debug("# " + expansion.display());
			for (Expansion parent : expansion.getParents()) {
				LOG.debug("Parent: " + parent.display());
			}
			for (Expansion child : expansion.getChildren()) {
				LOG.debug("Child: " + child.display());
			}
		}
		
		assertEquals(limitedDepthExpansions.size(), expansionStrings.size());

		// depth test (4)
		depth = 4;
		String[] depth4Additions = new String[] {
				" de sa deuxième femme"
		};
		
		List<String> depth4Expansions = new ArrayList<String>();
		for (String depth3Expansion : depth3Expansions) {
			if (depth3Expansion.endsWith("maternelle")) {
				for (String depth4Addition : depth4Additions) {
					String newExpansion = depth3Expansion + depth4Addition;
					depth4Expansions.add(newExpansion);
				}
			}
		}
		limitedDepthExpansions.addAll(depth4Expansions);

		LOG.debug("Expected expansions depth " + depth + ":");
		for (String expansionString : limitedDepthExpansions){
			LOG.debug(expansionString);
		}

		expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
		termExtractor.setMaxDepth(depth);
		expansions = termExtractor.getExpansions(chat, configuration, 0, expansionsPerNoun);
		expansionStrings = new TreeSet<String>();
		for (Expansion expansion : expansions) {
			expansionStrings.add(expansion.display());
		}
		
		LOG.debug("Actual expansions depth " + depth + ":");
		for (String expansionString : expansionStrings){
			LOG.debug(expansionString);
		}

		for (String expansion : limitedDepthExpansions) {
			assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
		}
		
		
		LOG.debug("Parents and children depth " + depth);
		for (Expansion expansion : expansions) {
			LOG.debug("# " + expansion.display());
			for (Expansion parent : expansion.getParents()) {
				LOG.debug("Parent: " + parent.display());
			}
			for (Expansion child : expansion.getChildren()) {
				LOG.debug("Child: " + child.display());
			}
		}
		assertEquals(limitedDepthExpansions.size(), expansionStrings.size());
	}
}
