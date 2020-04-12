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

import gnu.getopt.Getopt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;

import util.RTSI;
import com.meapsoft.featextractors.AvgMelSpec;

/**
 * Abstract class that all MEAPsoft utilities must extend.  Defines
 * some global constants and useful static methods.  Based on Mike
 * Mandel's Meap.java
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 * @author Mike Mandel (mim@ee.columbia.edu)
 */
public abstract class MEAPUtil implements Runnable
{
    public static final String version = "1.1.1";
	
    // output file name
    String outFile;
    
    // slash
	public static final String slash = System.getProperty("file.separator");
	
	// error messages
	//public static final int FATAL_ERROR = 0;
	//public static final int MESSAGE = 1;
	
    // Audio format parameters
    public static final int numChannels = 1;
    public static final int bitsPerSamp = 16;
    public static final int samplingRate = 44100;
    //public static final int samplingRate = 22050;
    public static final boolean signed = true;
    public static final boolean bigEndian = false;
    public static final AudioFormat stereo = new AudioFormat(samplingRate, 
                                                             bitsPerSamp, 2, 
                                                             signed, bigEndian);

    AudioFormat format = new AudioFormat(samplingRate, bitsPerSamp, 
                                         numChannels, signed, bigEndian);

    // Various "global" numerical parameters:
    public static final int frameLatency = 200;
 
    public static int mixerToUse = 0;

    // Should we write the output MEAPFile(s)?  - not always necessary
    // when using the GUI.  Defaults to true.
    public boolean writeMEAPFile = true;

    // Should we print verbose output?  
    // Default is false because this might be called from a GUI. 
    protected boolean verbose = false;

    // keep track of this MEAPsoft utlity's progress
    protected BoundedRangeModel progress = new DefaultBoundedRangeModel();
    
    protected static ExceptionHandler exceptionHandler = new ExceptionHandler();


    // All MEAPUtils need to implement this interface:
    // void setup()  (optional - this class implements an empty version)
    // void run()

    public void setup() throws IOException, ParserException
        { }

    /**
     * Set everything up, process input, and write output.
     */
    public abstract void run(); 

    public static void printCommandLineOptions(char arg)
    {
        switch(arg)
        {
        case 'f':
            System.out.println(
              "    -f feat_name    use feat_name features (defaults to AvgMelSpec, can have multiple -f arguments)\n" +
              "Supported feat_names are: " +
              "");
            RTSI.find("com.meapsoft.featextractors", "com.meapsoft.featextractors.FeatureExtractor");
            break;
        case 'd':
            System.out.println(
              "    -d dist_metric  distance metric to use (defaults to EuclideanDist, can string them together with multiple -d arguments)\n" +
              "Supported distance metrics are: " +
              "");
            // this doesn't work unless you have a package name...
            RTSI.find("com.meapsoft", "com.meapsoft.ChunkDist");
            break;
        case 'i':
            System.out.println(
              "    -i feat_dim     what feature dimentions to use (defaults to all)\n" +  
              "                    where feat_dim is a comma separated list (no spaces)\n" +
              "                    of integer indices and ranges (e.g. 1-5,7:9,11)" + 
              "");
            break;
        }
    }

    public static void printCommandLineOptions(char[] args)
    {
        for(int x = 0; x < args.length; x++)
            printCommandLineOptions(args[x]);
    }


    /**
     * Parse arguments common to many MEAPUtils - array of feature
     * dimensions.
     */
    public static int[] parseFeatDim(String[] args, String argString)
    {
        Vector features = new Vector();

        Getopt opt = new Getopt("MEAPUtil", args, argString);
        opt.setOpterr(false);
        
        int c = -1;
        while ((c = opt.getopt()) != -1) 
        {
            if(c == 'i') 
            {
                String[] dims = opt.getOptarg().split(",");
                for(int x = 0; x < dims.length; x++)
                {
                    String[] range = dims[x].split("[:-]",2);
                    int start = Integer.parseInt(range[0]);
                    features.add(new Integer(start));

                    if(range.length > 1)
                    {
                        int end = Integer.parseInt(range[1]);
                        for(int y = start+1; y <= end; y++)
                            features.add(new Integer(y));
                    }
                }
            }
        }

        if(features.size() != 0)
        {
            //once again Java's fucked up attitude toward primitive
            //types bites me in the ass
            int[] featdim = new int[features.size()];

            for(int x = 0; x < featdim.length; x++)
                featdim[x] = ((Integer)features.get(x)).intValue();

            return featdim;
        }

        return null;
    }


    /**
     * Parse arguments common to many MEAPUtils - Distance metrics
     */
    public static ChunkDist parseChunkDist(String[] args, String argString, int[] featdim)
    {
        ChunkDist dist = null;

        Getopt opt = new Getopt("MEAPUtil", args, argString);
        opt.setOpterr(false);
        
        int c = -1;
        while ((c = opt.getopt()) != -1) 
        {
            if(c == 'd') 
            {
                String className = opt.getOptarg();

                // Try to load the class named className that extends
                // ChunkDist.  (This is ugly as hell)
                Class cl = null;
                try 
                { 
                    cl = Class.forName(className);
                }
                catch(ClassNotFoundException e)
                { 
                    System.out.println(e);
                }

                try 
                {
                    if(cl != null 
                       && Class.forName("ChunkDist").isAssignableFrom(cl))
                    {
                        try
                        {
                            //ChunkDist cd = (ChunkDist)cl.newInstance(featdim);
                            ChunkDist cd = null;
                            if(featdim == null)
                                 cd = (ChunkDist)cl.newInstance();
                            else
                            {
                                // it is ridiculous how complicated it
                                // is to get this done
                                Class[] ctargs = new Class[1];
                                Object arg = Array.newInstance(Integer.TYPE, 
                                                               featdim.length);
                                ctargs[0] = arg.getClass();
                                Constructor ct = cl.getConstructor(ctargs);

                                for(int x = 0; x < featdim.length; x++)
                                    Array.setInt(arg, x, featdim[x]);

                                Object[] o = new Object[1];
                                o[0] = arg;
                                cd = (ChunkDist)ct.newInstance(o);
                            }
                            
                            if(dist == null)
                                dist = cd;
                            else
                            {
                                // tack cd on to the end of the list

                                // of ChunkDists specified in dist
                                ChunkDist curr = dist;
                                for(curr = dist; curr.next != null; curr = curr.next);
                                curr.next = cd;
                            }
                        }
                        catch(Exception e)
                        {
                            System.out.println("Error constructing ChunkDist "
                                               + className);
                            e.printStackTrace();
                        }
                    }   
                    else
                        System.out.println("Ignoring unknown distance metric: " 
                                           + className);
                }
                catch(ClassNotFoundException e)
                {
                    System.out.println("This should never ever happen....");
                    e.printStackTrace();
                }
            }
        }

        // default
        if(dist == null)
            dist = new EuclideanDist(featdim);

        return dist;
    }


    /**
     * Parse arguments common to many MEAPUtils - feature extractors
     */
    public static Vector parseFeatureExtractor(String[] args)
    {
        return MEAPUtil.parseFeatureExtractor(args, "f:");
    }


    /**
     * Parse arguments common to many MEAPUtils - feature extractors
     */
    public static Vector parseFeatureExtractor(String[] args, String argString)
    {
        Vector featExts = new Vector();

        Getopt opt = new Getopt("MEAPUtil", args, argString);
        opt.setOpterr(false);
        
        int c = -1;
        while ((c = opt.getopt()) != -1) 
        {
            if(c == 'f')
            {
                String featName = opt.getOptarg();
                
                // Try to load the class named featName that extends
                // featextractors.FeatureExtractor.  (This is ugly as
                // hell)
                Class cl = null;
                try 
                { 
                    cl = Class.forName("com.meapsoft.featextractors." + featName);
                }
                catch(ClassNotFoundException e)
                { 
                    try { cl = Class.forName(featName); }
                    catch(ClassNotFoundException e2)
                        { System.out.println(e2); }
                }

                try 
                {
                    if(cl != null 
                       && Class.forName("com.meapsoft.featextractors.FeatureExtractor").isAssignableFrom(cl))
                    {
                        try
                        {
                            featExts.add(cl.newInstance());
                            //feat_names += featName + " ";
                        }
                        catch(Exception e)
                        {
                            System.out.println("Error constructing FeatureExtractor "
                                               + featName);
                            e.printStackTrace();
                        }
                    }   
                    else
                        System.out.println("Ignoring unknown feature: " + featName);
                }
                catch(ClassNotFoundException e)
                {
                    System.out.println("This should never ever happen....");
                    e.printStackTrace();
                }
            }
        }

        // default to AvgMelSpec
        if(featExts.size() == 0)
            featExts.add(new AvgMelSpec());

        return featExts;
    }

    public AudioWriter openAudioWriter() throws LineUnavailableException {
        SourceDataLine line = null;
        AudioWriter writer = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                               format); 
        
        Mixer.Info[] m1x0rs = AudioSystem.getMixerInfo();
        //System.out.println("Do you support " + info + "?");
        for(int i=0; i<m1x0rs.length; i++)
            if(AudioSystem.getMixer(m1x0rs[i]).isLineSupported(info))
                mixerToUse = i;
        
        // Obtain and open the line.
        line = (SourceDataLine) AudioSystem.getMixer(m1x0rs[mixerToUse])
            .getLine(info); 
        //line.open(format, 1024*5); 
        line.open(format);
        //System.out.println("Source line opened from mixer: " 
        //+ m1x0rs[mixerToUse]); 
        writer = new AudioWriter(line);
        
        return writer;
    }
    
    public AudioWriter openAudioWriter(String filename) throws LineUnavailableException, IOException {
        if(filename == null)
            return openAudioWriter();
        
        AudioWriter writer = null;
        File file = new File(filename);
        AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
        String extension = null;
        
        // Figure out the file extension
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1)
            extension = filename.substring(dotPosition + 1);
        
        if(extension != null) {
            // Figure out the corresponding file type
            AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
            for (int i = 0; i < aTypes.length; i++)
                if (aTypes[i].getExtension().equals(extension))
                    targetType = aTypes[i];
        }
        
        writer = new AudioWriter(file, format, targetType);
        
        return writer;
    }
    
    // Get an input stream from an audio file
    public AudioInputStream openInputStream(String filename) throws IOException, UnsupportedAudioFileException
    {
        return openInputStream(filename, format);
    }

    // Get an input stream from an audio file
    public static AudioInputStream openInputStream(String filename, AudioFormat format) throws IOException, UnsupportedAudioFileException
    {
        AudioInputStream stream = null;

        stream = AudioSystem.getAudioInputStream(new File(filename));
        //System.out.println(stream.getFormat());
        
        AudioFormat baseFormat = stream.getFormat();
        AudioFormat decodedFormat = 
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            bigEndian);
        stream = AudioSystem.getAudioInputStream(decodedFormat, stream);
        //System.out.println(stream.getFormat());

        // convert to stereo PCM before converting to mono -
        // without this line the conversion process will crap out
        // on stereo MP3s.  But the conversion from mono to stereo
        // is unsupported for some reason...
        if(stream.getFormat().getChannels() >= 2)
            //stream = AudioSystem.getAudioInputStream(MEAPUtil.stereo, stream);
            stream = AudioSystem.getAudioInputStream(
                new AudioFormat(format.getSampleRate(), bitsPerSamp, 2, 
                                signed, bigEndian), 
                stream);
        
        stream = AudioSystem.getAudioInputStream(format, stream);

        //System.out.println("final: "+stream.getFormat());
        
        return stream;
    }

    // Convert a byte stream into a stream of doubles.  If it's stereo,
    // the channels will be interleaved with each other in the double
    // stream, as in the byte stream.
    public static void bytes2doubles(byte[] audioBytes, double[] audioData, AudioFormat format) 
     {
         if (format.getSampleSizeInBits() == 16) 
         {
            if (format.isBigEndian()) 
            {
                for (int i = 0; i < audioData.length; i++) 
                {
                    /* First byte is MSB (high order) */
                    int MSB = (int) audioBytes[2*i];
                    /* Second byte is LSB (low order) */
                    int LSB = (int) audioBytes[2*i+1];
                    audioData[i] = ((double)(MSB << 8 | (255 & LSB))) 
                        / 32768.0;
                }
            } 
            else 
            {
                for (int i = 0; i < audioData.length; i++) 
                {
                    /* First byte is LSB (low order) */
                    int LSB = (int) audioBytes[2*i];
                    /* Second byte is MSB (high order) */
                    int MSB = (int) audioBytes[2*i+1];
                    audioData[i] = ((double)(MSB << 8 | (255 & LSB))) 
                        / 32768.0;
                }
            }
         } 
         else if (format.getSampleSizeInBits() == 8) 
         {
             int nlengthInSamples = audioBytes.length;
             if (format.getEncoding().toString().startsWith("PCM_SIGN")) 
             {
                 for (int i = 0; i < audioBytes.length; i++) 
                     audioData[i] = audioBytes[i] / 128.0;
             } 
             else 
             {
                 for (int i = 0; i < audioBytes.length; i++) 
                     audioData[i] = (audioBytes[i] - 128) / 128.0;
             }
         }
     }
    
    // Convert an array of doubles into a  byte stream
    public static void doubles2bytes(double[] audioData, byte[] audioBytes, AudioFormat format) 
    {
        int in;
        if (format.getSampleSizeInBits() == 16) 
        {
            if (format.isBigEndian()) 
            {
                for (int i = 0; i < audioData.length; i++) 
                {
                    in = (int)(audioData[i]*32767);
                    /* First byte is MSB (high order) */
                    audioBytes[2*i] = (byte)(in >> 8);
                    /* Second byte is LSB (low order) */
                    audioBytes[2*i+1] = (byte)(in & 255);
                }
            } 
            else 
            {
                for (int i = 0; i < audioData.length; i++) 
                {
                    in = (int)(audioData[i]*32767);
                    /* First byte is LSB (low order) */
                    audioBytes[2*i] = (byte)(in & 255);
                    /* Second byte is MSB (high order) */
                    audioBytes[2*i+1] = (byte)(in >> 8);
                }
            }
        } 
        else if (format.getSampleSizeInBits() == 8) 
        {
            if (format.getEncoding().toString().startsWith("PCM_SIGN")) 
            {
                for (int i = 0; i < audioData.length; i++) 
                    audioBytes[i] = (byte)(audioData[i]*127);
            } 
            else 
            {
                for (int i = 0; i < audioData.length; i++)
                    audioBytes[i] = (byte)(audioData[i]*127 + 127);
            }
        }
    }

    public void setExceptionHandler(ExceptionHandler eh)
    {
        exceptionHandler = eh;
    }

    /**
     * Get the BoundedRangeModel that is keeping track of this
     * MEAPUtil's progress.
     */
    public BoundedRangeModel getProgress()
    {
        return progress;
    }
    
	public static String[] getPaths()
	{
		String meapsoftDirectory = null;
		String dataDirectory = null;
		
		try
		{
			//This method will get the path to the jar or class file no-matter 
			//where the jar is called from.
	
			String javaclasspath = System.getProperty("java.class.path")
				.split(System.getProperty("path.separator"))[0];
	
			File binPath = new File(javaclasspath);
	
			binPath = binPath.getCanonicalFile().getParentFile();
			//System.out.println("Path: " + binPath.toString()); 
					
			meapsoftDirectory = binPath.getParent();
			dataDirectory = meapsoftDirectory + slash + "data";
			
			//System.out.println("meapsoftDirectory: " + meapsoftDirectory + " dataDirectory: " + dataDirectory);
		}
		catch (Exception e)
		{
			System.out.println("problem getting paths! " + e.toString());
			//ShowDialog(e, "", FATAL_ERROR);
			return null;
		}	
		
		String[] paths = {meapsoftDirectory, dataDirectory};
		return paths;
	}
}

