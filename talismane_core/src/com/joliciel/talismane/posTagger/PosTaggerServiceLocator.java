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

import com.joliciel.talismane.TalismaneServiceLocator;

public class PosTaggerServiceLocator {
	PosTaggerServiceImpl posTaggerService = null;
	private TalismaneServiceLocator talismaneServiceLocator;
	
	public PosTaggerServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}
	
	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = new PosTaggerServiceImpl();
			posTaggerService.setPosTaggerFeatureService(this.talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService());
			posTaggerService.setPosTaggerService(this.talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService());
			posTaggerService.setTokeniserService(this.talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService());
			posTaggerService.setMachineLearningService(this.talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
			posTaggerService.setFeatureService(this.talismaneServiceLocator.getFeatureServiceLocator().getFeatureService());
		}
		return posTaggerService;
	}

	public TalismaneServiceLocator getTalismaneServiceLocator() {
		return talismaneServiceLocator;
	}
}
