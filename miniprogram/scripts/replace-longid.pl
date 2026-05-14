#!/usr/bin/env perl
use strict;
use warnings;

# Replace ID-like number fields with LongId in miniprogram TS files.
# 用法: perl scripts/replace-longid.pl <file.ts>

my $file = $ARGV[0];
die "Usage: perl replace-longid.pl <file>" unless $file;

local $/;
open my $fh, '<', $file or die "open $file: $!";
my $content = <$fh>;
close $fh;
my $orig = $content;
my $had_longid_already = ($orig =~ /\bLongId\b/m);

# camelCase 字段以 Id 结尾, 或裸 id
my $pat = qr/(\b(?:\w+Id|id))(\??)\s*:\s*number\b/;
$content =~ s/$pat/$1$2: LongId/g;

if ($content ne $orig) {
  unless ($had_longid_already) {
    # hard-coded alias to avoid Perl @ interpolation issues
    if ($content =~ /from\s+['"]\Qaaaccc\E\/types['"]/) {
      # placeholder, never matches
    }
    my $core_alias_re = quotemeta('@core/types');
    if ($content =~ /from\s+['"]$core_alias_re['"]/) {
      $content =~ s/(import\s+(?:type\s+)?\{)([^}]*?)(\s*\}\s*from\s+['"]$core_alias_re['"])/$1$2, LongId$3/m;
    } elsif ($content =~ /^import /m) {
      my $line = "import type { LongId } from " . "'" . '@core/types' . "'" . "\n";
      $content =~ s/^(import [^\n]*\n)/$line$1/m;
    } else {
      $content = "import type { LongId } from " . "'" . '@core/types' . "'" . "\n\n" . $content;
    }
  }
  open my $wfh, '>', $file or die "write $file: $!";
  print $wfh $content;
  close $wfh;
  print "MOD $file\n";
}
