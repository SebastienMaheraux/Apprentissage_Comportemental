#!/usr/bin/python
import scrapy
import re

from doctissimo.items import DoctissimoItem

class TestSpider(scrapy.Spider):
    name = "test"
    allowed_domains = ["forum.doctissimo.fr"]
    start_urls = [
        "http://forum.doctissimo.fr/nutrition/alimentation-sante/liste_sujet-1.htm",
    ]


    def parse(self, response):
        for page_index in range(2,471):
            print "page %d: %s" %(page_index, response.url)
            next_page_url = re.sub(r'liste_sujet-[0-9]*\.htm$', "liste_sujet-" + `page_index + 1` + ".htm", response.url)
            print "next page parsed: ", next_page_url
            yield scrapy.Request(next_page_url, callback=self.parse)
