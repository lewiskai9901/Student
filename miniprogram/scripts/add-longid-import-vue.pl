#!/usr/bin/env perl
use strict;
use warnings;

my $file = $ARGV[0];
die "Usage: perl add-longid-import-vue.pl <file>" unless $file;
local $/;
open my $fh, '<', $file or die "open $file: $!";
my $content = <$fh>;
close $fh;
my $orig = $content;

# Skip if no LongId reference or already has import
exit 0 unless $content =~ /\bLongId\b/m;
exit 0 if $content =~ /import type \{ LongId \}/m;

# Use single-quoted heredoc-style for the import line
my $importLine = "import type { LongId } from " . "'" . '@core/types' . "'" . "\n";
$content =~ s/(<script setup[^>]*>\n)/$1$importLine/m;

if ($content ne $orig) {
  open my $w, '>', $file or die "write $file: $!";
  print $w $content;
  close $w;
  print "MOD $file\n";
}
