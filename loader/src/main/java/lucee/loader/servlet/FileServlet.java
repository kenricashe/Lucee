/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.loader.servlet;

import java.io.IOException;

import /* JAVJAK */ javax.servlet.ServletConfig;
import /* JAVJAK */ javax.servlet.ServletException;
import /* JAVJAK */ javax.servlet.http.HttpServletRequest;
import /* JAVJAK */ javax.servlet.http.HttpServletResponse;

import lucee.loader.engine.CFMLEngineFactory;

/**
 */
public class FileServlet extends AbsServlet {

	private static final long serialVersionUID = 1555107078656945805L;

	@Override
	public void init(final ServletConfig sg) throws ServletException {
		super.init(sg);
		engine = CFMLEngineFactory.getInstance(sg, this);
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
		engine.serviceFile(this, req, rsp);
	}
}