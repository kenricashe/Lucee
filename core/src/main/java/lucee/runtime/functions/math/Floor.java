/**
 *
 * Copyright (c) 2016, Lucee Assosication Switzerland. All rights reserved.
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
package lucee.runtime.functions.math;

import java.math.RoundingMode;

import lucee.runtime.PageContext;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.listener.AppListenerUtil;
import lucee.runtime.op.Caster;

public class Floor extends BIF {

	private static final long serialVersionUID = 8816436870378089996L;

	public static Number call(PageContext pc, Number number) {
		if (AppListenerUtil.getPreciseMath(pc, null)) {
			return Caster.toBigDecimal(number).setScale(0, RoundingMode.FLOOR);
		}
		return Math.floor(Caster.toDoubleValue(number));
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if (args.length == 1) return call(pc, Caster.toDoubleValue(args[0]));
		throw new FunctionException(pc, "Floor", 1, 1, args.length);
	}
}
