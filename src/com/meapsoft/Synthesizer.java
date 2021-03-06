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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Program that processes a MEAPsoft EDL file and synthesizes audio
 * data from it.  This supports audio playbacl or saving the audio
 * data to a wav file.
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 */
public class Synthesizer extends MEAPUtil
{
    EDLFile edlFile;
    // if outFile is null, sound will be output to the first available
    // audio mixer
    String outFile = null;
    AudioWriter audioWriter;

    // output buffer
    double[] outSamples;
    int outSamplesLength;
    
    // we want to synthesize cd quality audio
    AudioFormat format = new AudioFormat(44100, bitsPerSamp, 
                                         numChannels, signed, bigEndian);

    public Synthesizer(String infile, String outfile)
    {
        this(new EDLFile(infile), outfile); 
    }

    public Synthesizer(EDLFile edl, String outfile)
    {
        edlFile = edl;
        outFile = outfile; 
    }
    
    // \todo{test output to line out}
    public void printUsageAndExit() 
    {
        System.out.println("Usage: Synthesizer [-options] file.edl \n\n" + 
                           "  where options include:\n" + 
                           "    -o output_file  output sound file (defaults to line out)\n" +
                           "");
        System.exit(0);
    }

    /**
     * Synthesizer constructor.  Parses command line arguments
     */
    public Synthesizer(String[] args) 
    {
        if(args.length == 0)
            printUsageAndExit();

        // Parse arguments
        Getopt opt = new Getopt("Synthesizer", args, "o:");
        opt.setOpterr(false);
        
        int c = -1;
        while ((c = opt.getopt()) != -1) 
        {
            switch(c) 
            {
            case 'o':
                outFile = opt.getOptarg();
                break;
            case '?':
                printUsageAndExit();
                break;
            default:
                System.out.print("getopt() returned " + c + "\n");
            }
        }
        
        // parse arguments
        int ind = opt.getOptind();
        if(ind > args.length)
            printUsageAndExit();
        
        edlFile = new EDLFile(args[ind]);

        System.out.println("Synthesizing " + outFile + " from " + args[ind] + ".");
    }

    public void setup() throws IOException, ParserException
    {
        if(!edlFile.haveReadFile)
            edlFile.readFile();

        if(edlFile.chunks.size() == 0)
            throw new ParserException(edlFile.filename, "No chunks found");

        // keep track of our progress in synthesizing this EDLFile:
        progress.setMinimum(0);
        progress.setMaximum(edlFile.chunks.size());
        progress.setValue(0);
    }

    public void processEDL() throws IOException, UnsupportedAudioFileException
    {
        // create output samples
        EDLChunk ch = (EDLChunk)edlFile.chunks.get(0); 
        outSamplesLength = (int)(format.getSampleRate()*(ch.dstTime + ch.length));

        outSamples = new double[outSamplesLength];
        // initialize outSamples properly
        Arrays.fill(outSamples, 0); 

        // we need to go through the chunks in ascending order of dstTime:
        ((Heap)(edlFile.chunks)).sort();

        Iterator i = edlFile.chunks.iterator();
        int overlapAccumulator = 0;
        while(i.hasNext())
        {
            EDLChunk currChunk = (EDLChunk)i.next();

            double[] chunkSamples = currChunk.getSamples(format);
            int offset = (int)(currChunk.dstTime*format.getSampleRate()) - overlapAccumulator;
            
            // \todo {should commands be parsed in Chunk.getSamples()?}
            for(int c = 0; c < currChunk.commands.size(); c++)
            {
                String cmd = (String)currChunk.commands.get(c);
                if(cmd.equalsIgnoreCase("reverse"))
                {
                    // reverse the current chunk in time
                    for(int x = 0; x < chunkSamples.length/2; x++)
                    {
                        double tmp = chunkSamples[chunkSamples.length-x-1];
                        chunkSamples[chunkSamples.length-x-1] = chunkSamples[x];
                        chunkSamples[x] = tmp;
                    }
                }
                // crossfade(time) (time in seconds) - add fade on
                // both ends of chunk (like the "fade" command) but
                // also overlaps it with the previous segment.
                // default overlap is 1ms;
                else if(cmd.toLowerCase().startsWith("crossfade"))
                {
                    int overlap = (int)(.005*format.getSampleRate());

                    String[] overlapTime = cmd.split("[(),\\s]");

                    if(overlapTime.length >= 2)
                        overlap = (int)(format.getSampleRate()*Double.parseDouble(overlapTime[1]));

                    if(offset - overlap > 0)
                    {
                        offset -= overlap;
                        overlapAccumulator += overlap;
                    }
                }
                // fade|crossfade(fadeInTime,fadeOutTime) (times in seconds)
                // if no arguments present defaults to 1ms fade in/out time
                // if 1 argument fadeInTime = fadeOutTime
                // \todo{crossfade is only allowed to have one argument but this is not enforced}
                else if(cmd.toLowerCase().startsWith("crossfade") || cmd.toLowerCase().startsWith("fade"))
                {
                    double fadeInTime = .005;
                    double fadeOutTime = .005;

                    String[] fadeTimes = cmd.split("[(),\\s]");

                    if(fadeTimes.length == 2)
                    {
                        fadeInTime = Double.parseDouble(fadeTimes[1]);
                        fadeOutTime = fadeInTime;
                    }
                    else if(fadeTimes.length > 2)
                    {
                        fadeInTime = Double.parseDouble(fadeTimes[1]);
                        fadeOutTime = Double.parseDouble(fadeTimes[2]);
                    }

                    // Smooth out any rough edges to the data with a
                    // simple triangular window on each end.
                    // \todo{Add support for fancier fade curves?}
                    int fadeInWinSize = (int)(fadeInTime*format.getSampleRate());
                    for(int x = 0; x < fadeInWinSize; x++) 
                    {
                        if(x < chunkSamples.length)
                            chunkSamples[x] *= (double)x / (double)fadeInWinSize;
                    }

                    int fadeOutWinSize = (int)(fadeOutTime*format.getSampleRate());
                    for(int x = 0; x < fadeOutWinSize; x++)
                    {
                        if(chunkSamples.length - 1 - x > 0)
                            chunkSamples[chunkSamples.length - 1 - x] 
                                *= (double)x / (double)fadeOutWinSize;
                    }
                }
                // gain(A) - scale the waveform by A.
                else if(cmd.toLowerCase().startsWith("gain"))
                {
					//scale the amplitudes
                    String[] gainString = cmd.split("[(),\\s]");
                    double gain = Double.parseDouble(gainString[1]);

                    for(int x = 0; x < chunkSamples.length; x++)
                        chunkSamples[x] *= gain;
                }
                else
                    System.out.println("Ignored unknown command: " + cmd);
            }
            // copy the current chunk's samples into the output buffer
            for(int x = 0; x < chunkSamples.length; x++)
                if(offset + x < outSamples.length)
                    outSamples[offset + x] += chunkSamples[x];

            progress.setValue(progress.getValue()+1);
        }

        outSamplesLength -= overlapAccumulator;
    }
    
    /**
     * Set everything up, process input, and write output.
     */
    public void run()
    {
        try 
        {
            doSynthesizer();
        }
        catch(Exception e) 
        { 
            exceptionHandler.handleException(e);
        }
    } 
    
    /**
     * Stop a running Synthesizer.  
     */
    public void stop()
    {
        try
        {
            audioWriter.close();
        }
        catch(IOException e) 
        { 
            exceptionHandler.handleException(e);
        }

        audioWriter = null;
    }

    public void doSynthesizer() throws IOException, ParserException, UnsupportedAudioFileException, LineUnavailableException
    {
        setup();
        processEDL();
        
        // write out the audio data
        //AudioWriter aw = new AudioWriter(outFile, format,  AudioFileFormat.Type.WAVE);
        audioWriter = openAudioWriter(outFile);
        
        audioWriter.write(outSamples, outSamplesLength);
        audioWriter.close();
    }

    public static void main(String[] args) 
    {
        Synthesizer o2or = new Synthesizer(args);
        long startTime = System.currentTimeMillis();
        o2or.verbose = true;
        o2or.run();
        System.out.println("Done.  Took " + 
                           ((System.currentTimeMillis() - startTime)/1000.0)
                           + "s");
        System.exit(0);
    }
}
