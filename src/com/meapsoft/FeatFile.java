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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a MEAPsoft segment/feature file.
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 */
public class FeatFile extends MEAPFile implements Cloneable
{
    // Keep track of the types (and dimensions) of features contained
    // in the chunks in this file
    public Vector featureDescriptions = new Vector();
    
    // The FeatChunks contained in this file
    public Vector chunks;  
    
    // regular expressions for parsing FeatFiles
    protected static final Pattern commentPattern = Pattern.compile(
        "#\\.*");
    protected static final Pattern linePattern = Pattern.compile(
        "\\s*([^#\\s]+)\\s*");
    // TODO: add feature weights and dimensions parsing into this pattern
    protected static final Pattern featDescPattern = Pattern.compile(
        "^#\\s*Features:\\s*");

    public FeatFile(String fn)
    {
        filename = fn;
        chunks = new Vector(100, 0);
    }

    // Java bitches if this is not present for some reason.  Don't use
    // it - its bad news.
    protected FeatFile() 
    { 
        this("BUG");
    }

    /**
     * Get a matrix (2D double array) of all of the features in all of
     * the chunks contained in this file.  Each row corresponds to a
     * single feature vector.  Index it as you would in Matlab
     * ([row][column] = [chunk][featdim]).
     */
    public double[][] getFeatures()
    {
        return getFeatures(null);
    }

    /**
     * Get a matrix (2D double array) of all of the features in all of
     * the chunks contained in this file.  Each row corresponds to
     * a single feature vector.  Index it as you would in Matlab
     * ([row][column] = [chunk][featdim]).
     */
    public double[][] getFeatures(int[] featdim)
    {
        // how many feature dimensions are we using?
        int maxdim = 0;
        if(featdim != null)
            maxdim = featdim.length;
        else
            maxdim = ((FeatChunk)chunks.get(0)).numFeatures();

        double[][] mat = new double[chunks.size()][maxdim];

        for(int x = 0; x < chunks.size(); x++)
        {
            FeatChunk c = (FeatChunk)chunks.get(x);

            double[] currFeat = c.getFeatures(featdim);
            for(int y = 0; y < currFeat.length; y++)
                mat[x][y] = currFeat[y];
        }

        return mat;
    }

	/**
	 * Return the number of elements in each feature
	 * 
	 * i.e. for AvgMelSpec (0-39) and AvgChroma (40-51) we return
	 * [40, 12]
	 * 
	 */
	
	public int[] getFeatureLengths()
	{
		int[] lengths = new int[featureDescriptions.size()];
		
		for(int i = 0; i < featureDescriptions.size(); i++)
		{
			int numDim = Integer.parseInt(
				((String)featureDescriptions.get(i)).split("[()]" )[1]);
			lengths[i] = numDim;
		}
		return lengths;
	}
	
    /**
     * Normalize the features contained in this file such that the
     * feature dimensions corresponding to the outputs of each
     * FeatureExtractor are normalized independently to lie between 0
     * and 1.  
     *
     * I.e. if this featfile contains outputs of both AvgMelSpec (dim
     * 0-39) and AvgChroma (dim 40-51), dimensions 0-39 will be
     * normalized independently of dimensions 40-51 and vice versa.
     */
    public void normalizeFeatures()
    {
        // operate on each feature type separately
        int startDim = 0;
        for(int featType = 0; featType < featureDescriptions.size(); featType++)
        {
            int numDim = Integer.parseInt(
                ((String)featureDescriptions.get(featType)).split("[()]" )[1]);

            int[] featDim = new int[numDim];
            for(int x = 0; x < numDim; x++)
                featDim[x] = startDim + x;

            // find the max and min values in these dimensions
            double[][] feat = getFeatures(featDim);
            double minFeat = DSP.min(DSP.min(feat));
            double maxFeat = DSP.max(DSP.max(feat));

            for(int x = 0; x < chunks.size(); x++)
            {
                FeatChunk c = (FeatChunk)chunks.get(x);

                double[] currFeat = c.getFeatures(featDim);

                for(int d = 0; d < featDim.length; d++)
                    c.setFeature(featDim[d], 
                                 (currFeat[d]-minFeat)/(maxFeat-minFeat));
            }

            startDim += numDim;
        }
    }

    /**
     * Apply weights (as listed in the "# Features: x.x*FeatureExtractor(ndim)" line )
     * to the features contained in this file.
     */
    public void applyFeatureWeights()
    {
        // operate on each feature type separately
        int startDim = 0;
        for(int featType = 0; featType < featureDescriptions.size(); featType++)
        {
            int numDim = Integer.parseInt(
                ((String)featureDescriptions.get(featType)).split("[()]")[1]);

            double weight = 1.0;
            try
            {
                weight = Double.parseDouble(
                    ((String)featureDescriptions.get(featType)).split("[*]")[0]);
            }
            catch(NumberFormatException e)
            {
                // the featureDescription does not contain a weight
                continue;
            }

            int[] featDim = new int[numDim];
            for(int x = 0; x < numDim; x++)
                featDim[x] = startDim + x;

            for(int x = 0; x < chunks.size(); x++)
            {
                FeatChunk c = (FeatChunk)chunks.get(x);

                double[] currFeat = c.getFeatures(featDim);

                for(int d = 0; d < featDim.length; d++)
                    c.setFeature(featDim[d], weight*currFeat[d]);
            }

            startDim += numDim;
        }
    }

    /**
     * Read in a feature file
     */
    public void readFile() throws IOException, ParserException
    {
        BufferedReader in  = new BufferedReader(new FileReader(filename));

        String audioFile;
        double chunkStartTime;
        double chunkLength;

        // each line (excluding comments) should look like: 
        // audioFile chunkStartTime chunkLength feature1 feature2 ...

        // Parse each line of the input file            
        boolean haveWrittenHeader = false;
        long lineno = 0;
        String line;
        while((line = in.readLine()) != null) 
        { 
            lineno++;
            
            // extract any comments from the current line
            String comment = "";
            Matcher c = commentPattern.matcher(line+"\n");
            if(c.find())
            {
                // comments go all the way to the end of the line
                comment = c.group() + line.substring(c.end()) + "\n";
                line = line.substring(0, c.start());

                // extract featureDescription from comment
                Matcher fd = featDescPattern.matcher(comment);
                if(fd.find())
                {
                	String featString = comment.substring(fd.end()).trim();

                    featureDescriptions.addAll(
                        new Vector(Arrays.asList(featString.split("\\s+"))));
                }
            }

            Matcher p = linePattern.matcher(line);
            // is there anything else?
            if(!p.find())
                continue;
            audioFile = p.group(1);
            // decode spaces in the file name
            audioFile = audioFile.replaceAll("%20", " ");
            //System.out.println(audioFile);

            if(!p.find())
                throw new ParserException(filename, lineno, 
                                          "Could not find chunk start time.");
            try { chunkStartTime = Double.parseDouble(p.group(1)); }
            catch(NumberFormatException nfe) { 
                throw new ParserException(filename, lineno, 
                                          "Could not parse chunk start time \"" 
                                          + p.group(1)  + "\".");  }
            if(!p.find())
                throw new ParserException(filename, lineno, 
                                          "Could not find chunk length.");
            try { chunkLength = Double.parseDouble(p.group(1)); }
            catch(NumberFormatException nfe) { 
                throw new ParserException(filename, lineno, 
                                          "Could not parse chunk length \"" 
                                          + p.group(1)  + "\".");  }
            
            FeatChunk ch = new FeatChunk(audioFile, chunkStartTime, chunkLength);
            ch.comment = comment;

            // save the remaining features on the line
            while(p.find())
            {
                // what kind of feature is this?  If its not a double then its a string;
                try 
                {
                    ch.addFeature(Double.parseDouble(p.group(1)));
                }
                catch (NumberFormatException e)
                {
                    ch.addFeature(p.group(1));
                }
            }

            chunks.add(ch);
        }

        in.close();
        haveReadFile = true;
    }
    
    /**
     *  Remove any chunks in this file
     */
    public void clearChunks()
    {
        chunks.clear();
    }

    /**
     *  Remove the features of all chunks in this file
     */
    public void clearFeatures()
    {
        Iterator i = chunks.iterator();
        while(i.hasNext())
            ((FeatChunk)i.next()).clearFeatures();
    }
    
    /**
     * Write the contents of this FeatFile
     */
    protected void write(Writer w) throws IOException
    {
        // write the header
        w.write("# filename onset_time chunk_length [features]\n# Features: ");
        
        for (int i = 0; i < featureDescriptions.size(); i++)
        {
	        w.write((String)featureDescriptions.elementAt(i));
        }
        w.write("\n");
        
        Iterator i = chunks.iterator();
        while(i.hasNext())
            w.write(i.next().toString());
    }

    /**
     * Clone this FeatFile
     */
    public Object clone()
    {
        FeatFile o = new FeatFile(this.filename);
       
        // superclass (MEAPFile) fields
        o.haveReadFile = this.haveReadFile;
        o.haveWrittenFile = this.haveWrittenFile;

        // local fields
        o.featureDescriptions = new Vector(this.featureDescriptions);

        o.chunks = new Vector(100);
        Iterator i = this.chunks.iterator();
        while(i.hasNext())
            o.chunks.add(((FeatChunk)i.next()).clone());

        return o;
    }

    /**
     *  Check if another FeatFile is compatible with this one.  Two
     *  feature files are said to be incompatible if they contain
     *  different features.
     */
    public boolean isCompatibleWith(FeatFile f)
    {
        //TODO:{Should really check if feature names match excluding
        //whitespace and other meaningless gunk.}

        //System.out.println(this.featureDescriptions.size()+" "+ f.featureDescriptions.size());

        if(this.featureDescriptions.size() != f.featureDescriptions.size())
            return false;

        for(int x = 0; x < this.featureDescriptions.size(); x++)
        {
            String featOne = ((String)this.featureDescriptions.elementAt(x)).trim();
            String featTwo = ((String)f.featureDescriptions.elementAt(x)).trim();

            //System.out.println(x+": "+featOne+" == "+ featTwo + " : " + featOne.equalsIgnoreCase(featTwo));

            if(!featOne.equalsIgnoreCase(featTwo))
                return false;
        }

        return true;
    }
}
