/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.umass.cs.mallet.base.classify;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Label;
import edu.umass.cs.mallet.base.types.LabelVector;
import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.util.ArrayListUtils;
import java.util.ArrayList;
import java.util.logging.*;

public abstract class Classifier
{
	private static Logger logger =
	Logger.getLogger("edu.umass.cs.mallet.base.classify.Classifier");

	protected Pipe instancePipe;

  /**
   * For serialization only.
   *
   */
  protected Classifier()
  {
  }

	public Classifier (Pipe instancePipe)
	{
		this.instancePipe = instancePipe;
		// All classifiers must have set of labels.
		assert (instancePipe.getTargetAlphabet() != null);
		assert (instancePipe.getTargetAlphabet().getClass().isAssignableFrom(LabelAlphabet.class));
		// Not all classifiers require a feature dictionary, however.
	}

	public Pipe getInstancePipe ()
	{
		return instancePipe;
	}

	public Alphabet getAlphabet ()
	{
		return (Alphabet) instancePipe.getDataAlphabet();
	}

	public LabelAlphabet getLabelAlphabet ()
	{
		return (LabelAlphabet) instancePipe.getTargetAlphabet();
	}

	public ArrayList classify (InstanceList instances)
	{
		ArrayList ret = new ArrayList (instances.size());
		InstanceList.Iterator iter = instances.iterator();
		while (iter.hasNext())
			ret.add (classify (iter.nextInstance()));
		return ret;
	}

	public Classification[] classify (Instance[] instances)
	{
		Classification[] ret = new Classification[instances.length];
		for (int i = 0; i < instances.length; i++)
			ret[i] = classify (instances[i]);
		return ret;
	}
		
	public abstract Classification classify (Instance instance);

	public Classification classify (Object obj)
	{
		if (obj instanceof Instance)
			return classify ((Instance)obj);
		return classify (new Instance (obj, null, null, null, instancePipe));
	}

	public double getAccuracy (InstanceList ilist)
	{
		int numCorrect = 0;
		for (int i = 0; i < ilist.size(); i++) {
			if (classify(ilist.getInstance(i)).bestLabelIsCorrect())
				numCorrect++;
		}
		return ((double)numCorrect/ilist.size());
	}

    
    // ArrayLists of Classifications are used to conserve computational cost.
    // Presumably one would want to know Precision, Recall, and F1 for one 
    // (maybe more) entry/index. Classifying once and storing the results in
    // an ArrayList reduces CPU overhead, doubly-so in the case of F1. 

        /** Calculate the precision of the classifier on an instance list for a
	    particular target entry */
        public double getPrecision( InstanceList ilist, Object entry )
        {
	    return getPrecision( this.classify(ilist), 
				 getLabelAlphabet().lookupIndex(entry, false) );
        }
    
        /** Calculate the precision for a particular target entry from an
	    array list of classifications */
        public double getPrecision( ArrayList classification, Object entry )
        {
	    return getPrecision( classification, 
				 getLabelAlphabet().lookupIndex(entry, false) );
        }

        /** Calculate the precision of the classifier on an instances list for a
	    particular target index */
        public double getPrecision( InstanceList ilist, int index )
        {
	    return getPrecision( this.classify(ilist), index );
	}

        /** Calculate the precision for a particular target index from an 
	    array list of classifications */
        public double getPrecision( ArrayList classification, int index )
        {
	    
	    int numCorrect = 0;
	    int numInstances = 0;
	    int trueLabel, classLabel;

	    for (int i = 0; i<classification.size(); i++) {

		trueLabel = ((LabelVector)(((Classification)classification.get(i)).getInstance()).getLabeling()).getBestIndex();
		classLabel = ((Classification)classification.get(i)).getLabeling().getBestIndex();
		
		if ( classLabel == index) {

		    numInstances++;

		    if ( trueLabel == index )
			
			numCorrect++;

		}
	    }

	    if (numInstances==0)
		logger.warning("No class instances: dividing by 0");
	    
	    return ((double)numCorrect/(double)numInstances);
		    
		    
	}

	/** Calculate the recall of the classifier on an instance list for a 
	    particular target entry */
        public double getRecall( InstanceList ilist, Object entry )
        {
	    return getRecall( this.classify(ilist), 
			      getLabelAlphabet().lookupIndex(entry, false) );
        }
    
        /** Calculate the recall of the classifier on an instance list for a
	    particular target index */
        public double getRecall( InstanceList ilist, int index )
        {
	    return getRecall( this.classify(ilist), index);
	}

        /** Calculate the recall for a particular target entry from an
	    array list of classifications */
        public double getRecall( ArrayList classification, Object entry )
        {
	    return getRecall( classification, 
			      getLabelAlphabet().lookupIndex(entry, false) );
	}

	/** Calculate the recall for a particular target index from an
	    array list of classifications */
        public double getRecall( ArrayList classification, int index )
        {
	    int numCorrect = 0;
	    int numInstances = 0;
	    int trueLabel, classLabel;

	    for (int i = 0; i<classification.size(); i++) {

		trueLabel = ((LabelVector)(((Classification)classification.get(i)).getInstance()).getLabeling()).getBestIndex();
		classLabel = ((Classification)classification.get(i)).getLabeling().getBestIndex();

		if ( trueLabel == index ) {

		    numInstances++;

		    if ( classLabel == index)
			
			numCorrect++;

		}
	    }

	    if (numInstances==0)
		logger.warning("No class instances: dividing by 0");

	    return ((double)numCorrect/(double)numInstances);
		    
		    
	}

        /** Calculate the F1-measure of the classifier on an instance list for a
	    particular target entry */
        public double getF1( InstanceList ilist, Object entry )
        {
	    return getF1( this.classify(ilist), 
			  getLabelAlphabet().lookupIndex(entry, false) );
        }
    
        /** Calculate the F1-measure of the classifier on an instance list for a
	    particular target index */
        public double getF1( InstanceList ilist, int index )
	{
	    return getF1(this.classify(ilist), index);
	}

        /** Calculate the F1-measure for a particular target entry from an
	    array list of classifications */
        public double getF1( ArrayList classification, Object entry )
        {
	    return getF1( classification, 
			  getLabelAlphabet().lookupIndex(entry, false) );
        }

        /** Calculate the F1-measure for a particular target index from an
	    array list of classifications */
        public double getF1( ArrayList classification, int index )
        {
	    double precision = getPrecision( classification, index );
	    double recall = getRecall( classification, index );
	    
	    if (precision==0.0 && recall==0.0)
		logger.warning("Precision and recall are 0: dividing by 0");
	    
	    return 2*precision*recall/(precision+recall);
	}

}
