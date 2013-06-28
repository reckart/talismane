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
package com.joliciel.talismane.parser;

import java.io.Reader;
import java.util.Set;

import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;

public interface ParserService {
	/**
	 * Gets the initial configuration for a particular pos-tagged token sequence.
	 * @param posTagSequence
	 * @return
	 */
	public ParseConfiguration getInitialConfiguration(PosTagSequence posTagSequence);

	public TransitionSystem getShiftReduceTransitionSystem();
	public TransitionSystem getArcEagerTransitionSystem();
	
	public DependencyArc getDependencyArc(PosTaggedToken head, PosTaggedToken dependent,
			String label);

	public CorpusEventStream getParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures);
	
	public NonDeterministicParser getTransitionBasedParser(MachineLearningModel<Transition> jolicielMaxentModel, int beamWidth, boolean dynamiseFeatures);
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker<Transition> decisionMaker, TransitionSystem transitionSystem, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth);

	public ParserEvaluator getParserEvaluator();
	public ParseComparator getParseComparator();

	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader);
}