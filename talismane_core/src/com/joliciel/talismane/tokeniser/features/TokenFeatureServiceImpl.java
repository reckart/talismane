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
package com.joliciel.talismane.tokeniser.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class TokenFeatureServiceImpl implements TokenFeatureService {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TokenFeatureServiceImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TokenFeatureServiceImpl.class);

	private FeatureService featureService;
	private MachineLearningService machineLearningService;
	private ExternalResourceFinder externalResourceFinder;

	TokeniserContextFeatureParser getTokeniserContextFeatureParser(List<TokenPattern> patternList) {
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(this.getFeatureService());
		parser.setPatternList(patternList);
		parser.setTokenFeatureParser(this.getTokenFeatureParser(patternList));
		return parser;
	}
	

	private TokenPatternMatchFeatureParser getTokenPatternMatchFeatureParser() {
		TokenPatternMatchFeatureParser parser = new TokenPatternMatchFeatureParser(this.getFeatureService());
		parser.setTokenFeatureParser(this.getTokenFeatureParser(null));
		return parser;
	}
	
	@Override
	public TokenFeatureParser getTokenFeatureParser() {
		return this.getTokenFeatureParser(null);
	}
	
	public TokenFeatureParser getTokenFeatureParser(List<TokenPattern> patternList) {
		TokenFeatureParserImpl tokenFeatureParser = new TokenFeatureParserImpl(this.getFeatureService());
		tokenFeatureParser.setPatternList(patternList);
		return tokenFeatureParser;
	}
	
	@Override
	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatureSet(List<String> featureDescriptors,
			List<TokenPattern> patternList) {
		Set<TokeniserContextFeature<?>> features = new TreeSet<TokeniserContextFeature<?>>();

		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		TokeniserContextFeatureParser tokeniserContextFeatureParser = this.getTokeniserContextFeatureParser(patternList);
		tokeniserContextFeatureParser.setPatternList(patternList);
		tokeniserContextFeatureParser.setExternalResourceFinder(externalResourceFinder);
		
		MONITOR.startTask("findFeatureSet");
		try {
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<TokeniserContextFeature<?>> myFeatures = tokeniserContextFeatureParser.parseDescriptor(functionDescriptor);
					MONITOR.startTask("add features");
					features.addAll(myFeatures);
					MONITOR.endTask("add features");
				}
			}
		} finally {
			MONITOR.endTask("findFeatureSet");
		}
		return features;
	}
	

	@Override
	public Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatureSet(
			List<String> featureDescriptors) {
		Set<TokenPatternMatchFeature<?>> features = new TreeSet<TokenPatternMatchFeature<?>>();

		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		TokenPatternMatchFeatureParser featureParser = this.getTokenPatternMatchFeatureParser();
		featureParser.setExternalResourceFinder(externalResourceFinder);
		
		MONITOR.startTask("findFeatureSet");
		try {
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<TokenPatternMatchFeature<?>> myFeatures = featureParser.parseDescriptor(functionDescriptor);
					MONITOR.startTask("add features");
					features.addAll(myFeatures);
					MONITOR.endTask("add features");
				}
			}
		} finally {
			MONITOR.endTask("findFeatureSet");
		}
		return features;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		if (this.externalResourceFinder==null) {
			this.externalResourceFinder = this.machineLearningService.getExternalResourceFinder();
		}
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(
			ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}


}
