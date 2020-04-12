#!/bin/bash

rm -f stairway*segments;
rm -f stairway*feat;
rm -f stairway*edl;

# STFTs take up lots of memory
JAVA="java -cp ../bin/MEAPsoft.jar -mx300m"
PFX="com.meapsoft"

$JAVA $PFX.Segmenter -o stairway.segments stairway.wav
$JAVA $PFX.FeatExtractor -f AvgSpecCentroid -f AvgSpecFlatness -o stairway.feat stairway.segments

# reverse the order of the chunks, so the chunks will play forwards,
# but in reverse order
# this should remind you of http://music.columbia.edu/~douglas/Nevaeh_Ot_Yawriats.mp3
perl sort_composer.pl -d 1 -r 1 stairway.feat stairway_revorder.edl
$JAVA $PFX.Synthesizer -o stairway_revorder.wav stairway_revorder.edl

# keep the chunk ordering, but reverse the samples in each chunk
# this should remind you of http://music.columbia.edu/~douglas/Yawriats_Ot_Nevaeh.mp3
perl sort_composer.pl -d 1 stairway.feat stairway_revsamples.edl reverse
$JAVA $PFX.Synthesizer -o stairway_revsamples.wav stairway_revsamples.edl

# sort the chunks in increasing order of power
# this is kind of lame, but it serves a proof of concept 
# (note that sort_composer defaults to sorting by the first feature)
#perl sort_composer.pl stairway.feat stairway_sortpower.edl
#$JAVA Synthesizer -o stairway_sortpower.wav stairway_sortpower.edl

perl sort_composer.pl -d 4 stairway.feat stairway_sortasc.edl
$JAVA $PFX.Synthesizer -o stairway_sortasc.wav stairway_sortasc.edl
