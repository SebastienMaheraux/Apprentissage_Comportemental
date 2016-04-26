#!/bin/bash

# remove all special and unwanted characters
#sed "s/\'/\\\'/g" -i ./test2.xml
# replace " by \" except for: <?xml version="1.0" encoding="utf-8"?>
sed 's/\"//g' -i ./test.xml
sed 's/<?xml version=1.0 encoding=utf-8?>/<?xml version=\"1.0\" encoding=\"utf-8\"?>/g' -i ./test.xml
# remove all <value> and </value> tags
sed 's/<\/*value>//g' -i ./test.xml
# replace the tag <items> by <forum category...>
sed 's/<items>/<forum category=\"foodHealth\">/g' -i ./test.xml
# replace the tag </items> by <forum>
sed 's/<\/items>/<\/forum>/g' -i ./test.xml
# replace starting tags by [tag_name]="
sed 's/<item>/\n<topic id=\"\" /g' -i ./test.xml
sed 's/<date>/date=\"/g' -i ./test.xml
sed 's/<message>/message=\"/g' -i ./test.xml
sed 's/<link>/link=\"/g' -i ./test.xml
sed 's/<title>/title=\"/g' -i ./test.xml
# replace ending tags by "[space] or " depending on the cases
sed 's/<\/item>/\/>/g' -i ./test.xml
sed 's/  <\/date>/\" /g' -i ./test.xml
sed 's/<\/message>/\" /g' -i ./test.xml
sed 's/<\/link>/\" /g' -i ./test.xml
sed 's/<\/title>/\" /g' -i ./test.xml
# in Atom, with atom-beautify installed, Ctrl-Alt-B to auto-format the XML
