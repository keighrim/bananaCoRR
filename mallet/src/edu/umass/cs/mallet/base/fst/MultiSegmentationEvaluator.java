/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
	 Evaluate segmentation f1 for several different tags (marked in OIB format).
	 For example, tags might be B-PERSON I-PERSON O B-LOCATION I-LOCATION O...

   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.umass.cs.mallet.base.fst;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.util.MalletLogger;
import java.io.*;
import java.util.logging.*;
import java.util.regex.*;
import java.text.DecimalFormat;

public class MultiSegmentationEvaluator extends TransducerEvaluator
{
	private static Logger logger = MalletLogger.getLogger(SegmentationEvaluator.class.getName());


	// equals() is called on these objects to determine if this token is the start or continuation of a segment.
	// A tag not equal to any of these is an "other".
	// is not part of the segment).
	Object[] segmentStartTags;
	Object[] segmentContinueTags;
	Object[] segmentStartOrContinueTags;
	
	private int evalIterations = 0;

	public MultiSegmentationEvaluator (Object[] segmentStartTags, Object[] segmentContinueTags, boolean showViterbi)
	{
		this.segmentStartTags = segmentStartTags;
		this.segmentContinueTags = segmentContinueTags;
		assert (segmentStartTags.length == segmentContinueTags.length);
    viterbiOutput = showViterbi;
	}

	public MultiSegmentationEvaluator (Object[] segmentStartTags, Object[] segmentContinueTags)
	{
    this(segmentStartTags, segmentContinueTags, true);
	}

	public boolean evaluate (Transducer model, boolean finishedTraining, int iteration,
													 boolean converged, double cost,
													 InstanceList training, InstanceList validation, InstanceList testing)
	{
		// Count the total number of evaluate calls, not the model's notion of iteration number,
		// because feature induction calls model.train() multiple times, and the iteration
		// number gets reset each time.
		iteration = evalIterations++;
		
		logger.info ("Evaluator Iteration="+iteration+" Cost="+cost);
		// Don't evaluate if it is too early in training to matter
		if (iteration < numIterationsToWait && !(alwaysEvaluateWhenFinished && finishedTraining))
			return true;
		// Only evaluate some iterations
		if (numIterationsToSkip > 0
				&& iteration % (numIterationsToSkip+1) != 0
				&& !(alwaysEvaluateWhenFinished && finishedTraining))
			return true;

		// Possibly write model to a file
		if (model instanceof Serializable && checkpointTransducer && iteration > 0
				&& iteration % (checkpointIterationsToSkip+1) == 0) {
			String checkFilename = checkpointFilePrefix == null ? "" : checkpointFilePrefix + '.';
			checkFilename = checkFilename + "checkpoint"+iteration+".model";
      try {
        ObjectOutputStream oos =
          new ObjectOutputStream(
            new FileOutputStream(new File(checkFilename)));
        oos.writeObject(model);
        oos.close();
        System.err.println("Model written to " + checkFilename);
      }
      catch (IOException e) {
        System.err.println("Exception writing file " + checkFilename + ": " + e);
      }
		}

		InstanceList[] lists = new InstanceList[] {training, validation, testing};
		String[] listnames = new String[] {"Training", "Validation", "Testing"};

		for (int k = 0; k < lists.length; k++)
			if (lists[k] != null)
      {
        PrintStream viterbiOutputStream = null;
        
        if (viterbiOutput && (iteration >= viterbiOutputIterationsToWait && iteration % (viterbiOutputIterationsToSkip+1) == 0)
            || (alwaysEvaluateWhenFinished && finishedTraining)) {
          if (viterbiOutputFilePrefix == null) {
            viterbiOutputStream = System.out;
          } else {
            String viterbiFilename = null;
            viterbiFilename = viterbiOutputFilePrefix + "."+listnames[k] + ".viterbi";
            try {
              FileOutputStream fos = new FileOutputStream (viterbiFilename);
              if (viterbiOutputEncoding == null)
                viterbiOutputStream = new PrintStream (fos);
              else
                viterbiOutputStream = new PrintStream (fos, true, viterbiOutputEncoding);
              //((CRF)model).write (new File(viterbiOutputFilePrefix + "."+description + iteration+".model"));
            } catch (IOException e) {
              logger.warning ("Couldn't open Viterbi output file '"+viterbiFilename+"'; continuing without Viterbi output trace.");
              viterbiOutputStream = null;
            }
          }
        }
        test(model, lists[k], listnames[k], viterbiOutputStream);
        if (viterbiOutputStream != null && viterbiOutputFilePrefix != null &&
            viterbiOutputStream != System.out)
          viterbiOutputStream.close();
      }
		if (printModelAtEnd && finishedTraining)
      model.toString();
		return true;
  }
	
  public void test(Transducer model, InstanceList data, String description,
                   PrintStream viterbiOutputStream)
  {
    int numCorrectTokens, totalTokens;
    int[] numTrueSegments, numPredictedSegments, numCorrectSegments;
    int allIndex = segmentStartTags.length;
    numTrueSegments = new int[allIndex+1];
    numPredictedSegments = new int[allIndex+1];
    numCorrectSegments = new int[allIndex+1];
    TokenSequence sourceTokenSequence = null;

    totalTokens = numCorrectTokens = 0;
    for (int n = 0; n < numTrueSegments.length; n++)
      numTrueSegments[n] = numPredictedSegments[n] = numCorrectSegments[n] = 0;
    for (int i = 0; i < data.size(); i++) {
      if (viterbiOutputStream != null)
        viterbiOutputStream.println ("Viterbi path for "+description+" instance #"+i);
      Instance instance = data.getInstance(i);
      Sequence input = (Sequence) instance.getData();
      //String tokens = null;
      //if (instance.getSource() != null)
      //tokens = (String) instance.getSource().toString();
      Sequence trueOutput = (Sequence) instance.getTarget();
      assert (input.size() == trueOutput.size());
      Sequence predOutput = model.viterbiPath(input).output();
      assert (predOutput.size() == trueOutput.size());
      int trueStart, predStart;				// -1 for non-start, otherwise index into segmentStartTag
      for (int j = 0; j < trueOutput.size(); j++) {
        totalTokens++;
        if (trueOutput.get(j).equals(predOutput.get(j)))
          numCorrectTokens++;
        trueStart = predStart = -1;
        // Count true segment starts
        for (int n = 0; n < segmentStartTags.length; n++) {
          if (segmentStartTags[n].equals(trueOutput.get(j))) {
            numTrueSegments[n]++;
            numTrueSegments[allIndex]++;
            trueStart = n;
            break;
          }
        }
        // Count predicted segment starts
        for (int n = 0; n < segmentStartTags.length; n++) {
          if (segmentStartTags[n].equals(predOutput.get(j))) {
            numPredictedSegments[n]++;
            numPredictedSegments[allIndex]++;
            predStart = n;
          }
        }
        if (trueStart != -1 && trueStart == predStart) {
          // Truth and Prediction both agree that the same segment tag-type is starting now
          int m;
          boolean trueContinue = false;
          boolean predContinue = false;
          for (m = j+1; m < trueOutput.size(); m++) {
            trueContinue = segmentContinueTags[predStart].equals (trueOutput.get(m));
            predContinue = segmentContinueTags[predStart].equals (predOutput.get(m));
            if (!trueContinue || !predContinue) {
              if (trueContinue == predContinue) {
                // They agree about a segment is ending somehow
                numCorrectSegments[predStart]++;
                numCorrectSegments[allIndex]++;
              }
              break;
            }
          }
          // for the case of the end of the sequence
          if (m == trueOutput.size()) {
            if (trueContinue == predContinue) {
              numCorrectSegments[predStart]++;
              numCorrectSegments[allIndex]++;
            }
          }
        }

        if (viterbiOutputStream != null) {
          FeatureVector fv = (FeatureVector) input.get(j);
          //viterbiOutputStream.println (tokens.charAt(j)+" "+trueOutput.get(j).toString()+
          //'/'+predOutput.get(j).toString()+"  "+ fv.toString(true));
          if (sourceTokenSequence != null)
            viterbiOutputStream.print (sourceTokenSequence.getToken(j).getText()+": ");
          viterbiOutputStream.println (trueOutput.get(j).toString()+
                                       '/'+predOutput.get(j).toString()+"  "+ fv.toString(true));
						
        }
      }
    }
    DecimalFormat f = new DecimalFormat ("0.####");
    logger.info (description +" tokenaccuracy="+f.format(((double)numCorrectTokens)/totalTokens));
    for (int n = 0; n < numCorrectSegments.length; n++) {
      logger.info ((n < allIndex ? segmentStartTags[n].toString() : "OVERALL") +' ');
      double precision = numPredictedSegments[n] == 0 ? 1 : ((double)numCorrectSegments[n]) / numPredictedSegments[n];
      double recall = numTrueSegments[n] == 0 ? 1 : ((double)numCorrectSegments[n]) / numTrueSegments[n];
      double f1 = recall+precision == 0.0 ? 0.0 : (2.0 * recall * precision) / (recall + precision);
      logger.info (" segments true="+numTrueSegments[n]+" pred="+numPredictedSegments[n]+" correct="+numCorrectSegments[n]+
                   " misses="+(numTrueSegments[n]-numCorrectSegments[n])+" alarms="+(numPredictedSegments[n]-numCorrectSegments[n]));
      logger.info (" precision="+f.format(precision)+" recall="+f.format(recall)+" f1="+f.format(f1));
    }

  }

}
