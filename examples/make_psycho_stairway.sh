#!/bin/bash

# This mashup sucks, but it serves as a test case for MashupComposer.
# Also I like the name psycho stairway

rm -f mashup*;
rm -f psycho_stairway*;

# STFTs take up lots of memory
JAVA="java -cp ../bin/MEAPsoft.jar -mx300m"
PFX="com.meapsoft"

$JAVA $PFX.Segmenter -o mashup_stairway.feat stairway.wav
$JAVA $PFX.Segmenter -o mashup_psycho_killer.feat psycho_killer.wav
$JAVA $PFX.FeatExtractor -f TimeCentroid -f AvgMelSpec mashup_stairway.feat mashup_psycho_killer.feat

# recreate psycho_killer using stairway chunks
$JAVA $PFX.composers.MashupComposer -o psycho_stairway.edl mashup_psycho_killer.feat mashup_stairway.feat 
# recreate stairway using psycho_killer chunks
#/$JAVA MashupComposer -f 1:40 -o psycho_stairway.edl mashup_stairway.feat mashup_psycho_killer.feat 
$JAVA $PFX.Synthesizer -o psycho_stairway.wav psycho_stairway.edl
