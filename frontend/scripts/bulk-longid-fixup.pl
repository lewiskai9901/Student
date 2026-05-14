#!/usr/bin/env perl
# Bulk fixup script: aggressively migrate ID-like number references to LongId
# in store / api / composable / util / view files (after types/ migration).
#
# Patterns applied:
#   1. `(\w+Id|id)\??\s*:\s*number` → `$1?: LongId`   (field/param declarations)
#   2. `Record<number,` → `Record<LongId,`            (map indexed by IDs)
#   3. `Map<number,` → `Map<LongId,`
#   4. `Number\(IDENT\.(id|\w+Id)\)` → `IDENT.id`     (remove redundant Number cast on IDs)
#   5. `Number\(IDENT\.(id|\w+Id)\s*\|\|\s*0\)` → `IDENT.id || ''`  (handle fallback)
#
# 加 import type { LongId } from '@/types/common' 如果文件被改且没已有 LongId.

use strict;
use warnings;

my $file = $ARGV[0];
die "Usage: perl bulk-longid-fixup.pl <file>" unless $file;

local $/;
open my $fh, '<', $file or die "open $file: $!";
my $content = <$fh>;
close $fh;
my $orig = $content;
my $had_longid_already = ($content =~ /\bLongId\b/m);

# Pattern 1: field/param declarations
$content =~ s/(\b(?:\w+Id|id))(\??)\s*:\s*number\b/$1$2: LongId/g;

# Pattern 2+3: Record/Map<number, ...> / Set<number>
$content =~ s/\bRecord<number,/Record<LongId,/g;
$content =~ s/\bMap<number,/Map<LongId,/g;
$content =~ s/\bSet<number>/Set<LongId>/g;

# Pattern 4+5: Number() casts on ID member access
# IDENT.id or IDENT.xxxId (with optional chaining or method access)
$content =~ s/\bNumber\(((?:[\w.!?]+\.)(?:id|\w+Id))\)/$1/g;
$content =~ s/\bNumber\(((?:[\w.!?]+\.)(?:id|\w+Id))\s*\|\|\s*0\)/$1 || ''/g;

# Pattern 4b: Number() casts on bare ID-like identifiers
# E.g. Number(sectionId), Number(planId), Number(id), Number(editingId.value)
$content =~ s/\bNumber\((\w+\.(?:value|id|\w+Id))\)/$1/g;
$content =~ s/\bNumber\((id|\w+Id)\)/$1/g;
$content =~ s/\bNumber\((id|\w+Id)\s*\|\|\s*0\)/$1 || ''/g;

# Pattern 6: ref<number | null>(null) / ref<number | undefined>(undefined) — many are IDs in this codebase
$content =~ s/\bref<number\s*\|\s*null>/ref<LongId | null>/g;
$content =~ s/\bref<number\s*\|\s*undefined>/ref<LongId | undefined>/g;

# Pattern 7: `as number` 类型断言 — when applied to an ID context, should be LongId
# 但这个很难区分真 number 与 ID. 保守: 仅当上下文有 .id / xxxId 时改.
# Skipped — too ambiguous.

# Pattern 8: `Array<number>` for ID arrays
$content =~ s/\bArray<number>(?=\s*[=;)\],])/Array<LongId>/g;
$content =~ s/\bnumber\[\](?=\s*=\s*\[[^,\]]*\.\w*[Ii]d)/LongId[]/g;

if ($content ne $orig) {
  unless ($had_longid_already) {
    if ($content =~ /from\s+['"]@\/types\/common['"]/) {
      $content =~ s/(import\s+(?:type\s+)?\{)([^}]*?)(\s*\}\s*from\s+['"]@\/types\/common['"])/$1$2, LongId$3/m;
    } elsif ($content =~ /^import /m) {
      # In .vue files, find <script> section's first import
      $content =~ s/^(import [^\n]*\n)/import type { LongId } from '\@\/types\/common'\n$1/m;
    } else {
      # No imports? unusual but possible — skip adding
    }
  }
  open my $wfh, '>', $file or die "write $file: $!";
  print $wfh $content;
  close $wfh;
  print "MOD $file\n";
}
