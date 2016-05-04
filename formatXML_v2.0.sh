#!/bin/bash

`cp $1 ./output.xml`

# remove all special and unwanted characters
#sed "s/\'/\\\'/g" -i ./test2.xml
# replace " by \" except for: <?xml version="1.0" encoding="utf-8"?>
sed 's/\"//g' -i ./output.xml
sed 's/<?xml version=1.0 encoding=utf-8?>/<?xml version=\"1.0\" encoding=\"utf-8\"?>/g' -i ./output.xml
# remove all <value> and </value> tags
sed 's/<\/*value>//g' -i ./output.xml
# replace the tag <items> by <forum category...>
sed 's/<items>/<forum category=\"foodHealth\">/g' -i ./output.xml
# replace the tag </items> by <forum>
sed 's/<\/items>/<\/forum>/g' -i ./output.xml
# replace starting tags by [tag_name]="
sed 's/<item>/\n<topic id=\"\" /g' -i ./output.xml
sed 's/<date>/date=\"/g' -i ./output.xml
sed 's/<message>/message=\"/g' -i ./output.xml
sed 's/<link>/link=\"/g' -i ./output.xml
sed 's/<title>/title=\"/g' -i ./output.xml
# replace ending tags by "[space] or " depending on the cases
sed 's/<\/item>/\/>/g' -i ./output.xml
sed 's/  <\/date>/\" /g' -i ./output.xml
sed 's/<\/message>/\" /g' -i ./output.xml
sed 's/<\/link>/\" /g' -i ./output.xml
sed 's/<\/title>/\" /g' -i ./output.xml
# in Atom, with atom-beautify installed, Ctrl-Alt-B to auto-format the XML
