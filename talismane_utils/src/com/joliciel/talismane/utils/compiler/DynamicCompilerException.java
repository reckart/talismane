///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.utils.compiler;

import com.joliciel.talismane.utils.JolicielException;

/**
 * Represents an exception thrown by the dynamic compiler.
 * @author Assaf Urieli
 *
 */
public class DynamicCompilerException extends JolicielException {
	private static final long serialVersionUID = 1L;

	public DynamicCompilerException(String message, Throwable cause) {
		super(message, cause);
	}

	public DynamicCompilerException(String message) {
		super(message);
	}

	public DynamicCompilerException(Throwable cause) {
		super(cause);
	}
	
}
