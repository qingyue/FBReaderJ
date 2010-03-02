/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.atom;

import java.util.*;


public class ATOMEntry extends ATOMCommonAttributes {

	public ATOMId Id;

	public List<ATOMAuthor> Authors;
	public List<ATOMCategory> Categories;
	//public final ATOMContent Content; TODO: implement ATOMContent
	public List<ATOMContributor> Contributors;
	public List<ATOMLink> Links;
	public ATOMPublished Published;
	public String Rights;  // TODO: implement ATOMTextConstruct
	//public final ATOMSource Source; // TODO: implement ATOMSource
	public String Summary; // TODO: implement ATOMTextConstruct
	public String Title;   // TODO: implement ATOMTextConstruct
	public ATOMUpdated Updated;

}