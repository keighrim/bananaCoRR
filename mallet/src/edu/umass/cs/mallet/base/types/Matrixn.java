/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.umass.cs.mallet.base.types; // Generated package name


/**
 * Matrixn.java
 *
 *  Implementation of Matrix that allows arbitrary
 *   number of dimensions.  This implementation
 *   simply uses a flat array.
 *
 * Created: Tue Sep 16 14:52:37 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version 1.0
 */
public class Matrixn extends DenseMatrix {

	int numDimensions;
	int[] sizes;

	/**
	 *  Create a 1-d matrix with the given values.
	 */
	public Matrixn(double[] vals) {
		numDimensions = 1;
		sizes = new int[1];
		sizes [0] = vals.length;
		values = (double[]) vals.clone();
	} 
	
	/**
	 *  Create a matrix with the given dimensions.
	 *
	 *  @param szs An array containing the maximum for
	 *      each dimension.
	 */
	public Matrixn (int szs[]) {
		numDimensions = szs.length;
		sizes = (int[])szs.clone();
		int total = 1;
		for (int j = 0; j < numDimensions; j++) {
	    total *= sizes [j];
		}
		values = new double [total];
	}
	
	/**
	 *  Create a matrix with the given dimensions and 
	 *   the given values.
	 *
	 *  @param szs An array containing the maximum for
	 *      each dimension.
	 *  @param vals A flat array of the entries of the
	 *      matrix, in row-major order.
	 */
	public Matrixn (int[] szs, double[] vals) {
		numDimensions = szs.length;
		sizes = (int[])szs.clone();
		values = (double[])vals.clone();
	}
	
	public int getNumDimensions () { return numDimensions; };
	
	public int getDimensions (int [] szs) {
		for ( int i = 0; i < numDimensions; i++ ) {
	    szs [i] = this.sizes [i];
		} 
		return numDimensions;
	}
	
	public double value (int[] indices) {
		return values [singleIndex (indices)];
	}
	
	public void setValue (int[] indices, double value) {
		values [singleIndex (indices)] = value;
	}

	public ConstantMatrix cloneMatrix () {
		/* The Matrixn constructor will clone the arrays. */
		return new Matrixn (sizes, values);
	}

	public int singleIndex (int[] indices) {
		int idx = 0;
		for ( int dim = 0; dim < numDimensions; dim++ ) {
			idx = (idx * sizes[dim]) + indices [dim];	   
		} 
		return idx;
	}

	public void singleToIndices (int single, int[] indices) {
		/* must be a better way to do this... */
		int size = 1;
		for (int i = 0; i < numDimensions; i++) {
	    size *= sizes[i];
		}
		for ( int dim = 0; dim < numDimensions; dim++) {
	    size /= sizes [dim];
	    indices [dim] = single / size;
	    single = single % size;
		} 
	}

	/* Test array referencing and dereferencing */
	public static void main(String[] args) {
		double m1[] = new double[] { 1.0, 2.0, 3.0, 4.0 };
		int idx1[] = new int[1];
		Matrixn a = new Matrixn (m1);
		System.out.println("Checking 1-D case");
		a.singleToIndices (3, idx1);
		System.out.println(idx1[0]);
		System.out.println (a.singleIndex (idx1));
		
		System.out.println ("Checking 2-D case");
		int sizes[] = new int[] { 2, 3 };
		m1 = new double [6];
		for (int i = 0; i < 6; i++) {
	    m1 [i] = 2.0 * i;
		}
		a = new Matrixn (sizes, m1);
		idx1 = new int [2];
		a.singleToIndices (5, idx1);
		System.out.println("5 => (" + idx1[0] + ", " + idx1[1] + ") => " + 
											 a.singleIndex (idx1) );
		System.out.println(a.value (idx1));
		
		System.out.println("Checking 3-D case");
		sizes = new int[] { 2, 3, 4 };
		idx1 = new int[3];
		m1 = new double [24];
		for (int i = 0; i < 24; i++) {
	    m1 [i] = 2.0 * i;
		}
		a = new Matrixn (sizes, m1);
		a.singleToIndices (21, idx1);
		System.out.println ("21 => (" + idx1[0] + " " + idx1[1] + " " +
												idx1[2] + ") =>" + a.singleIndex (idx1));
		System.out.println(a.value (idx1));
	} 
    
} 
