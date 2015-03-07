/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
	 Create new features from features (matching a regex within a window +/- the current position.

   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.umass.cs.mallet.base.pipe.tsf;

import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.PropertyList;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.Instance;
import java.io.*;
import java.util.regex.*;

public class FeaturesInWindow extends Pipe implements Serializable
{
	String namePrefix, namePrefixLeft;
	int leftBoundary;
	int rightBoundary;
	Pattern featureRegex;
	boolean includeBeginEndBoundaries;
	boolean includeCurrentToken = false;

	private static final int maxWindowSize = 20;
	private static final PropertyList[] startfs = new PropertyList[maxWindowSize];
	private static final PropertyList[] endfs = new PropertyList[maxWindowSize];
	
	static {
		initStartEndFs ();
	}

	private static void initStartEndFs ()
	{
		for (int i = 0; i < maxWindowSize; i++) {
			startfs[i] = PropertyList.add ("<START"+i+">", 1.0, null);
			endfs[i] = PropertyList.add ("<END"+i+">", 1.0, null);
		}
	}
	
	public FeaturesInWindow (String namePrefix, int leftBoundaryOffset, int rightBoundaryOffset,
													 Pattern featureRegex, boolean includeBeginEndBoundaries)
	{
		this.namePrefix = namePrefix;
		this.leftBoundary = leftBoundaryOffset;
		this.rightBoundary = rightBoundaryOffset;
		this.featureRegex = featureRegex;
		this.includeBeginEndBoundaries = includeBeginEndBoundaries;
	}

	public FeaturesInWindow (String namePrefix, int leftBoundaryOffset, int rightBoundaryOffset)
	{
		this (namePrefix, leftBoundaryOffset, rightBoundaryOffset, null, true);
	}
	
	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		int tsSize = ts.size();
		PropertyList[] newFeatures = new PropertyList[tsSize];
		for (int i = 0; i < tsSize; i++) {
			Token t = ts.getToken (i);
			PropertyList pl = t.getFeatures();
			newFeatures[i] = pl;
			for (int position = i + leftBoundary; position < i + rightBoundary; position++) {
				if (position == i && !includeCurrentToken)
					continue;
				PropertyList pl2;
				if (position < 0)
					pl2 = startfs[-position];
				else if (position >= tsSize)
					pl2 = endfs[position-tsSize];
				else
					pl2 = ts.getToken(position).getFeatures ();
				PropertyList.Iterator pl2i = pl2.iterator();
				while (pl2i.hasNext()) {
					pl2i.next();
					String key = pl2i.getKey();
					if (featureRegex != null && featureRegex.matcher(key).matches()) {
						newFeatures[i] = PropertyList.add ((namePrefixLeft == null || position-i>0 ? namePrefix : namePrefixLeft)+key,
																							 pl2i.getNumericValue(), newFeatures[i]);
					}
				}
			}
			// Put the new PropertyLists in place
			t.setFeatures (newFeatures[i]);
		}
		return carrier;
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeObject (namePrefix);
		out.writeInt (leftBoundary);
		out.writeInt (rightBoundary);
		out.writeObject (featureRegex);
		out.writeBoolean (includeBeginEndBoundaries);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		namePrefix = (String) in.readObject();
		leftBoundary = in.readInt ();
		rightBoundary = in.readInt ();
		featureRegex = (Pattern) in.readObject();
		includeBeginEndBoundaries = in.readBoolean();
	}

}
