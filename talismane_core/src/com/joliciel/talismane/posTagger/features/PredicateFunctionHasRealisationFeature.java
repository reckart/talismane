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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PredicateArgument;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * For this pos-tagged token's main lexical entry, assuming the function name provided is in the list,
 * does it have the realisation provided?
 * If the function name provided is not in the list, returns null.
 * @author Assaf Urieli
 *
 */
public final class PredicateFunctionHasRealisationFeature<T> extends AbstractPosTaggedTokenFeature<T,Boolean> implements BooleanFeature<T> {
	public StringFeature<T> functionNameFeature;
	public StringFeature<T> realisationNameFeature;

	public PredicateFunctionHasRealisationFeature(PosTaggedTokenAddressFunction<T> addressFunction, StringFeature<T> functionNameFeature, StringFeature<T> realisationNameFeature) {
		super(addressFunction);
		this.functionNameFeature = functionNameFeature;
		this.realisationNameFeature = realisationNameFeature;
		this.setName(super.getName() + "(" + functionNameFeature.getName() + "," + realisationNameFeature.getName() + ")");
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<String> functionNameResult = this.functionNameFeature.check(context, env);
		FeatureResult<String> realisationNameResult = this.realisationNameFeature.check(context, env);
		if (functionNameResult!=null && realisationNameResult!=null) {
			String functionName = functionNameResult.getOutcome();
			String realisationName = realisationNameResult.getOutcome();
			LexicalEntry lexicalEntry = null;
			if (posTaggedToken.getLexicalEntries().size()>0)
				lexicalEntry = posTaggedToken.getLexicalEntries().iterator().next();
			if (lexicalEntry!=null) {
				PredicateArgument argument = lexicalEntry.getPredicateArgument(functionName);
				if (argument!=null) {
					boolean hasRealisation = argument.getRealisations().contains(realisationName);
					featureResult = this.generateResult(hasRealisation);
				}
			}
		}
		return featureResult;
	}
	
	
}
