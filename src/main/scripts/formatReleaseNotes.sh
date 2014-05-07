#!/bin/sh

usage()
{
    echo "Usage: `basename $0` <changelog.md file> [<output file>]"
    exit 1
}

if [ $# -lt 1 ]; then
    echo "Error: missing argument"
    usage
fi

CHANGELOG_MD=$1
if [ ! -f ${CHANGELOG_MD} ]; then
    echo "Error: changelog file ${CHANGELOG_MD} does not exist"
    exit 2
fi
shift
OUTPUT_FILE=""
if [ $# -ge 1 ]; then
    OUTPUT_FILE=$1
fi

# first weed out the non-relevant comments
grep 'Feeds Catalog\|R00\|feedscatalog-\|[BD]-[0-9][0-9][0-9][0-9][0-9]\|^$' ${CHANGELOG_MD} > $$.out

# format is like this:
# 2013-10-15 12:00:12 -0500    B-40572: remove SSL transformation on LBaaS usage event (Shinta Smith)
# \1   \2 \3 \4        \5      \6     \7                                               

sed "s/\(.*\)-\(.*\)-\(.*\) \(.*\) -\([0-9 ]*\)\(.*\)\([BD]\)-\(.*\)/\1-\2-\3 | \6\7-\8/" $$.out > $$.out.2
sed "s/# Changelog for Usage Schema\(.*\)/# Changelog for Usage Schema/" $$.out.2 > $$.out

if [ ! -z ${OUTPUT_FILE} ]; then
    cp $$.out ${OUTPUT_FILE}
else
    cat $$.out
fi

rm -rf $$.out*
