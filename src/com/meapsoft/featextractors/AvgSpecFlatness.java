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

package com.meapsoft.featextractors;

import com.meapsoft.STFT;

/**
 * A simple feature calculation that averages the spectral flatness of
 * all spectral frames in a given chunk.  The feature varies between 0
 * and 1 with 1 being perfectly flat.
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 */
public class AvgSpecFlatness extends FeatureExtractor 
{
	
    public double[] features(STFT stft, long startFrame, int length) 
    {
        double[] curFrame;
        double[] avgSpecFlatness= new double[1];
        double num = 1;
        double den = Double.MIN_VALUE;

        avgSpecFlatness[0] = 0;

        for(int frame=0; frame<length; frame++) 
        {
            num = 0; den = 0;
            int nband = stft.getRows();
            curFrame = stft.getFrame(startFrame+frame);
            for(int band = 0; band < nband; band++) 
            {
                // convert back to linear power
                double p = Math.pow(10,curFrame[band]/10);

                num += Math.log(p)/nband;
                den += p/nband;
            }
            avgSpecFlatness[0] += Math.exp(num)/(den*length);
        }
        
        return avgSpecFlatness;
    }

	public String description()
	{
		return "Provides a measure of the peakiness of the chunks average spectrum.";
	}
}
