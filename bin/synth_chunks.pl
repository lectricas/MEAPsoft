#!/usr/bin/perl

# Synthesize each chunk in an EDL file in its own wav file.
#
# to use do this:
# synth_chunks.pl input.edl output_base_name


$arg_num = 0;


foreach $arg (@ARGV)
{
	if ($arg_num == 0)
	{
		$EDL_input_filename = $arg;
	}
	elsif ($arg_num == 1)
	{
		$WAV_filename_base = $arg;
	}
	$arg_num++;
}

print "input edl: $EDL_input_filename\n";
print "output wav: $WAV_filename_base\n";

if (! open INPUT_FILE, $EDL_input_filename)
{
	die "can't open file! $!";
}
else
{
	print "yay, I opened it!\n";
}


$line_number = 0;

while ($line = <INPUT_FILE>)
{

	$index = index $line, " ";
	$length = length($line);
	$end_string = substr($line, $index, $length);
	$new_string = "0.0$end_string";
	#print "$new_string";


	$EDL_output_filename = "$EDL_input_filename$line_number.edl";
	$WAV_output_filename = "$WAV_filename_base$line_number.wav";

	if (! open OUTPUT_FILE, ">$EDL_output_filename")
	{
		die "can't open output file! $!";
	}

	print OUTPUT_FILE "$new_string";

	if ($line_number > 0)
	{
		#$command = "java -cp MEAPsoft.jar com.meapsoft.Synthesizer -o $WAV_output_filename $EDL_output_filename\n";
		system "java -cp MEAPsoft.jar com.meapsoft.Synthesizer -o $WAV_output_filename $EDL_output_filename";
		print $command;
	}

	unlink($EDL_output_filename);

	$line_number++;
}

