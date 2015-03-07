/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.umass.cs.mallet.base.util;

import java.util.*;
import java.io.*;
import edu.umass.cs.mallet.base.util.BshInterpreter;

public abstract class CommandOption
{
	static BshInterpreter interpreter;
	
	Class owner;
	java.lang.String name;
	java.lang.String argName;
	Class argType;												// if never an argument, this null
	boolean argRequired;
	java.lang.String shortdoc;
	java.lang.String longdoc;
	java.lang.String fullName;
	boolean invoked = false;							// did this command option get processed, or do we just have default value

	public CommandOption (Class owner, java.lang.String name, java.lang.String argName,
												Class argType, boolean argRequired,
												java.lang.String shortdoc, java.lang.String longdoc)
	{
		this.owner = owner;
		this.name = name;
		this.argName = argName;
		this.argType = argType;
		this.argRequired = argRequired;
		this.shortdoc = shortdoc;
		this.longdoc = longdoc;
    Package p = owner.getPackage();
		this.fullName = (p != null ? p.toString() : "") + name;
		if (interpreter == null)
			interpreter = new BshInterpreter ();
	}

	// Deprecated
	public CommandOption (Class owner, java.lang.String name, java.lang.String argName,
												Class argType, boolean argRequired,
												java.lang.String shortdoc)
	{
		this (owner, name, argName, argType, argRequired, shortdoc, null);
	}
	
	/** Return the next unprocessed index. */
	public int process (java.lang.String[] args, int argi)
	{
		//System.out.println (name + " processing arg "+args[argi]);
		if (argi >= args.length ||
        args[argi] == null || args[argi].length() < 2 ||
        args[argi].charAt(0) != '-' || args[argi].charAt(1) != '-')
      return argi;
		java.lang.String optFullName = args[argi].substring(2);
		int dotIndex = optFullName.lastIndexOf('.');
		java.lang.String optName = optFullName;
		if (dotIndex != -1) {
			java.lang.String optPackageName = optFullName.substring (0, dotIndex);
			if (owner.getPackage() != null &&
          !owner.getPackage().toString().endsWith(optPackageName))
				return argi;
			optName = optFullName.substring (dotIndex+1);
		}
		if (!name.equals(optName))
			return argi;
		// The command-line option at "argi" is this one.
		this.invoked = true;
		argi++;
		if (args.length > argi && (args[argi].length() < 2
															 || (args[argi].charAt(0) != '-' && args[argi].charAt(1) != '-'))) {
			argi = parseArg (args, argi);
		} else {
			if (argRequired) {
				throw new IllegalArgumentException ("Missing argument for option " + optName);
			} else {
				// xxx This is not parallel behavior to the above parseArg(String[],int) method.
				parseArg ("");
			}
			argi++;
		}
		return argi;
	}

	public static BshInterpreter getInterpreter ()
	{
		return interpreter;
	}

	public java.lang.String getFullName ()
	{
		return fullName;
	}

	public abstract java.lang.String defaultValueToString();

	public boolean wasInvoked ()
	{
		return invoked;
	}

	public int parseArg (java.lang.String args[], int argi)
	{
		parseArg (args[argi]);
		return argi+1;
	}

	public void parseArg (java.lang.String arg)
	{
	}

	/** To be overridden by subclasses;
	    "list" is the the CommandOption.List that called this option */
	public void postParsing (CommandOption.List list)
	{
	}

	/** For objects that can provide CommandOption.List's (which can be merged into other lists. */
	public static interface ListProviding
	{
		public CommandOption.List getCommandOptionList ();
	}

	public static class List
	{
		ArrayList options;
		HashMap map;
		java.lang.String generaldoc;

		private List (java.lang.String generaldoc) {
			this.options = new ArrayList ();
			this.map = new HashMap ();
			this.generaldoc = generaldoc;
			
			add (new Boolean (CommandOption.class, "help", "TRUE|FALSE", false, false,
												"Print this command line option usage information.  "+
												"Give argument of TRUE for longer documentation", null)
				{ public void postParsing(CommandOption.List list) { printUsage(value); System.exit(-1); } });
			add (new Object	(CommandOption.class, "prefix-code", "'JAVA CODE'", true, null,
											 "Java code you want run before any other interpreted code.  Note that the text "+
											 "is interpretted without modification, so unlike some other Java code options, "+
											 "you need to include any necessary 'new's.", null));
		}

		public List (java.lang.String generaldoc, CommandOption[] options) {
			this(generaldoc);
			add(options);
		}

		public int size ()
		{
			return options.size();
		}

		public CommandOption getCommandOption (int index)
		{
			return (CommandOption) options.get(index);
		}

		public void add (CommandOption opt) {
			options.add (opt);
			map.put (opt.getFullName(), opt);
		}

		public void add (CommandOption[] opts) {
			for (int i = 0; i < opts.length; i++)
				add (opts[i]);
		}

		public void add (CommandOption.List opts) {
			for (int i = 0; i < opts.size(); i++)
				add (opts.getCommandOption(i));
		}
		
		public void process (java.lang.String[] args)
		{
			int argi = 0;
			while (argi < args.length) {
				int newArgi = argi;
				for (int i = 0; i < options.size(); i++) {
					CommandOption o = (CommandOption)options.get(i);
					newArgi = o.process (args, argi);
					if (newArgi != argi) {
						o.postParsing(this);
						break;
					}
				}
				if (newArgi == argi) {
					// All of the CommandOptions had their chance to claim the argi'th option,,
					// but none of them did.
					printUsage(false);
					throw new IllegalArgumentException ("Unrecognized option "+args[argi]);
				}
				argi = newArgi;
			}
		}

		public int processOptions (java.lang.String[] args)
		{
			for (int argi = 0; argi < args.length;) {
				int newArgi = argi;
				for (int i = 0; i < options.size(); i++) {
					CommandOption o = (CommandOption)options.get(i);
					newArgi = o.process (args, argi);
					if (newArgi != argi) {
						o.postParsing(this);
						break;
					}
				}
				if (newArgi == argi) {
          if (argi < args.length && args[argi].length() > 1 &&
              args[argi].charAt(0) == '-' && args[argi].charAt(1) == '-') {
            printUsage(false);
            throw new IllegalArgumentException ("Unrecognized option "+args[argi]);				}
          return argi;
        }
				argi = newArgi;
			}
      return args.length;
		}

		public void printUsage (boolean printLongDoc)
		{
			// xxx Fix this to have nicer formatting later.
			System.err.println (generaldoc);
			for (int i = 0; i < options.size(); i++) {
				CommandOption o = (CommandOption) options.get(i);
				System.err.println ("--"+ o.name + " " + o.argName + "\n  " + o.shortdoc);
				if (o.longdoc != null && printLongDoc)
					System.err.println ("  "+o.longdoc);
				System.err.println ("  Default is "+o.defaultValueToString());
			}
		}

	}

	
	public static class Boolean extends CommandOption
	{
		public boolean value, defaultValue;;
		public Boolean (Class owner, java.lang.String name, java.lang.String argName,
										boolean argRequired, boolean defaultValue,
										java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, Boolean.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public boolean value () { return value; }
		public void parseArg (java.lang.String arg) {
			if (arg == null || arg.equalsIgnoreCase("true") || arg.equals("1"))
				value = true;
			else if (arg.equalsIgnoreCase("false") || arg.equals("0"))
				value = false;
			else
				throw new IllegalArgumentException ("Boolean option should be true|false|0|1.  Instead found "+arg);
		}
		public java.lang.String defaultValueToString() { return java.lang.Boolean.toString(defaultValue); }
	}

	public static class Integer extends CommandOption
	{
		public int value, defaultValue;
		public Integer (Class owner, java.lang.String name, java.lang.String argName,
										boolean argRequired, int defaultValue,
										java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, Integer.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public int value () { return value; }
		public void parseArg (java.lang.String arg) { value = java.lang.Integer.parseInt(arg); }
		public java.lang.String defaultValueToString() { return java.lang.Integer.toString(defaultValue); }
	}

	public static class IntegerArray extends CommandOption
	{
		public int[] value, defaultValue;
		public IntegerArray (Class owner, java.lang.String name, java.lang.String argName,
										boolean argRequired, int[] defaultValue,
										java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, IntegerArray.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public int[] value () { return value; }
		public void parseArg (java.lang.String arg) {
      java.lang.String elts[] = arg.split(",");
      value = new int[elts.length];
      for (int i = 0; i < elts.length; i++)
        value[i] = java.lang.Integer.parseInt(elts[i]);
    }
		public java.lang.String defaultValueToString() {
      StringBuffer b = new StringBuffer();
      java.lang.String sep = "";
      for (int i = 0; i < defaultValue.length; i++) {
        b.append(sep).append(java.lang.Integer.toString(defaultValue[i]));
        sep = ",";
      }
      return b.toString();
    }
	}

	public static class Double extends CommandOption
	{
		public double value, defaultValue;
		public Double (Class owner, java.lang.String name, java.lang.String argName,
									 boolean argRequired, double defaultValue,
									 java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, Double.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public double value () { return value; }
		public void parseArg (java.lang.String arg) { value = java.lang.Double.parseDouble(arg); }
		public java.lang.String defaultValueToString() { return java.lang.Double.toString(defaultValue); }
	}

	public static class String extends CommandOption
	{
		public java.lang.String value, defaultValue;
		public String (Class owner, java.lang.String name, java.lang.String argName,
									 boolean argRequired, java.lang.String defaultValue,
									 java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, java.lang.String.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public java.lang.String value () { return value; }
		public void parseArg (java.lang.String arg) { value = arg; }
		public java.lang.String defaultValueToString() { return defaultValue; }
	}

	public static class SpacedStrings extends CommandOption
	{
		public java.lang.String[] value, defaultValue;
		public SpacedStrings (Class owner, java.lang.String name, java.lang.String argName,
													boolean argRequired, java.lang.String[] defaultValue,
													java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, java.lang.String.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public java.lang.String[] value () { return value; }
		public int parseArg (java.lang.String args[], int argi)
		{
			int count = 0;
			this.value = null;
			while (argi < args.length
						 && (args[argi].length() < 2
								 || (args[argi].charAt(0) != '-' && args[argi].charAt(1) != '-'))) {
				count++;
				java.lang.String[] oldValue = value;
				value = new java.lang.String[count];
				if (oldValue != null)
					System.arraycopy (oldValue, 0, value, 0, oldValue.length);
				value[count-1] = args[argi];
				argi++;
			}
			return argi;
		}
		public java.lang.String defaultValueToString() {
			if (defaultValue == null)
				return "(null)";
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < defaultValue.length; i++) {
				sb.append (defaultValue[i]);
				if (i < defaultValue.length-1)
					sb.append (" ");
			}
			return sb.toString();
		}
	}
	
	public static class File extends CommandOption
	{
		public java.io.File value, defaultValue;
		public File (Class owner, java.lang.String name, java.lang.String argName,
								 boolean argRequired, java.io.File defaultValue,
								 java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, java.io.File.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public java.io.File value () { return value; }
		public void parseArg (java.lang.String arg) { value = new java.io.File(arg); }
		public java.lang.String defaultValueToString() { return defaultValue == null ? null : defaultValue.toString(); }
	}

	// Value is a string that can take on only a limited set of values
	public static class Set extends CommandOption
	{
		public java.lang.String value, defaultValue;
		java.lang.String[] setContents;
		java.lang.String contentsString;
		public Set (Class owner, java.lang.String name, java.lang.String argName,
								boolean argRequired, java.lang.String[] setContents, int defaultIndex,
								java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, java.io.File.class, argRequired, shortdoc, longdoc);
			this.value = setContents[defaultIndex];
			this.setContents = setContents;
			StringBuffer sb = new StringBuffer ();
			for (int i = 0; i < setContents.length; i++) {
				sb.append (setContents[i]);
				sb.append (",");
			}
			this.contentsString = sb.toString();
		}
		public java.lang.String value () { return value; }
		public void parseArg (java.lang.String arg)
		{
			value = null;
			for (int i = 0; i < setContents.length; i++)
				if (setContents[i].equals(arg))
					value = setContents[i];
			if (value == null)
				throw new IllegalArgumentException ("Unrecognized option argument \""+arg+"\" not in set "+contentsString);
		}
		public java.lang.String defaultValueToString() { return defaultValue; }
	}


	public static class Object extends CommandOption
	{
		public java.lang.Object value, defaultValue;
		public Object (Class owner, java.lang.String name, java.lang.String argName,
									 boolean argRequired, java.lang.Object defaultValue,
									 java.lang.String shortdoc, java.lang.String longdoc)
		{
			super (owner, name, argName, java.lang.Object.class, argRequired, shortdoc, longdoc);
			this.defaultValue = value = defaultValue;
		}
		public java.lang.Object value () { return value; }
		public void parseArg (java.lang.String arg) {
			try {
				value = interpreter.eval (arg);
			} catch (bsh.EvalError e) {
				throw new IllegalArgumentException ("Java interpreter eval error\n"+e);
			}
		}
		public java.lang.String defaultValueToString() { return defaultValue == null ? null : defaultValue.toString(); }
	}

	
}
