/*
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */

package com.meapsoft;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * Representation of a FeatFile Chunk
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 */

public class FeatChunk extends Chunk implements Cloneable
{
    // List of features associated with this chunk.
    protected Vector features = null;

    /**
     * FeatChunk constructor
     */
    public FeatChunk(String sf, double st, double l)
    {
        super(sf, st, l);
        
        features = new Vector();
    }
   
    /**
     * Add the given features to this chunk.
     */
    public void addFeature(Object[] f)
    {
        features.addAll(Arrays.asList(f));
    }

    /**
     * Add the given features to this chunk.
     */
    public void addFeature(Collection f)
    {
        features.addAll(f);
    }

    /**
     * Add the given features to this chunk.
     */
    public void addFeature(double[] f)
    {
        // Java Fucking Sucks. why in gods name does the Double class need
        // to exist?  Why can't all instances of doubles in Object context
        // be automatically promoted to Doubles.  Why damnit why.  (This
        // is actually solved in Java 1.5...  They call it autoboxing)
        for(int i = 0; i < f.length; i++)
            features.add(new Double(f[i]));
    }

    /**
     * Add the given features to this chunk.
     */
    public void addFeature(Object f)
    {
        features.add(f);
    }

    /**
     * Add the given features to this chunk.
     */
    public void addFeature(double f)
    {
        features.add(new Double(f));
    }

    /**
     * Set feature dimension idx to the given feature value.
     */
    public void setFeature(int idx, double f)
    {
        features.set(idx, new Double(f));
    }

    /**
     * Set feature dimension idx to the given feature value.
     */
    public void setFeatures(double[] f)
    {
        features = new Vector(f.length);
        for(int x = 0; x < f.length; x++)
            features.add(x, new Double(f[x]));
    }

    /**
     * Returns the number of features associated with this chunk
     */
    public int numFeatures()
    {
        return features.size();
    }

    /**
     * Get the features associated with this chunk
     */
    public double[] getFeatures()
    {
        double[] feats = new double[features.size()];

        for(int i = 0; i < feats.length; i++)
            feats[i] = ((Double)features.get(i)).doubleValue();

        return feats;
    }
    
    /**
     * Get the subset of features corresponding to the dimensions
     * listed in idx.
     */
    public double[] getFeatures(int[] idx)
    {
        if(features == null)
            return null;

        if (idx == null)
            return getFeatures();

        double[] feats = new double[idx.length];

        for(int i = 0; i < idx.length; i++)
            feats[i] = ((Double)features.get(idx[i])).doubleValue();

        return feats;
    }

    /**
     *  Remove the features associated with this Chunk
     */
    public void clearFeatures()
    {
        features.clear();
    }

    /**
     * Write a description of this Chunk as a String in the format
     * expected in a FeatFile
     */
    public String toString()
    {
        // concatenating strings is super slow.  Better to use StringBuffer
        // guesstimate the string length
        StringBuffer s = new StringBuffer(20*features.size());
        s.append(srcFile.replaceAll(" ", "%20")).append(" ").append(startTime).append(" ").append(length).append(" ");

        if(features != null)
        {
            Iterator x = features.iterator();
            while(x.hasNext())
                    s.append(x.next()).append(" ");
        }

        s.append(comment).append("\n");
        
        return s.toString();
    }

    /**
     * Clone this FeatChunk
     */
    public Object clone()
    {
        FeatChunk o = new FeatChunk(this.srcFile, this.startTime, this.length);
        o.comment = this.comment;
        o.features = (Vector)this.features.clone();

        return o;
    }
}
