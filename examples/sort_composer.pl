#!/usr/bin/perl

use Getopt::Std;

die "Usage: perl sort_composer.pl options feat_file edl_file command\n
   where options include:
   -d sort_dim   column of the feature file to sort over
                 (defaults to 3 - first feature dimension)
   -r 1          to sort in reverse order\n" if $#ARGV < 1;

my %args;
getopt("r:d:", \%args);

$feat_file = shift;
$edl_file = shift;
$command = shift;
$sort_dim = 3;
$sort_dim = $args{'d'} if exists $args{'d'};

open FD, "<", $feat_file;
@feats = <FD>;
close FD;

print "Composing $edl_file from $feat_file.\n";

# store the EDL in a hash so we can sort later;
my %edl = ();

foreach(@feats)
{
    # strip out comments
    if( /^([^\#]+)/ )
    {
        @fields = split /\s+/, $1;
        
        # don't care about features, just need chunk location info:
        
        #$edl{$fields[$sort_dim]} = "$fields[0] $fields[1] $fields[2]";
        # need to use the value im sorting over as values instead of
        # keys because they are not guaranteed to be unique
        $edl{"$fields[0] $fields[1] $fields[2]"} = $fields[$sort_dim];
    } 
}

# output it all in sorted order
open FD, ">", $edl_file;
$currtime = 0;
@sorted = sort { $edl{$a} <=> $edl{$b} } keys %edl;
@sorted = reverse @sorted if exists $args{'r'};
for $key (@sorted)
{
    print FD "$currtime $key $command    \# key: $edl{$key} \n";

    @tmp = split /\s+/, $key;
    $currtime += $tmp[$#tmp]; 
}
close FD;

