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
package com.joliciel.talismane.posTagger;

import java.util.List;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A non-deterministic pos tagger, which analyses multiple tokenising possibilities for this sentence,
 * and returns multiple postagging possibilities.
 * @author Assaf Urieli
 *
 */
public interface NonDeterministicPosTagger extends PosTagger {
	/**
	 * Analyse a list of token sequences, each of which represents one possibility of tokenising a given sentence,
	 * and return the n most likely pos tag sequences for the sentence.
	 * @param tokenSequences the n most likely token sequences for this sentence.
	 * @return the n most likely postag sequences for this sentence
	 */
	public abstract List<PosTagSequence> tagSentence(List<TokenSequence> tokenSequences);
	

	/**
	 * The maximum number of possible sequences returned by the pos-tagger.
	 * @return
	 */
	public abstract int getBeamWidth();
}