#!/usr/bin/python
import scrapy
import re

from doctissimo.items import DoctissimoItem

class DoctissimoSpider(scrapy.Spider):
    name = "doctissimo"
    allowed_domains = ["forum.doctissimo.fr"]
    start_urls = [
        "http://forum.doctissimo.fr/nutrition/alimentation-sante/liste_sujet-1.htm",
    ]
    #def start_requests(self):
    #    yield scrapy.Request('http://www.example.com/1.html', self.parse)
    #    yield scrapy.Request('http://www.example.com/2.html', self.parse)
    #    yield scrapy.Request('http://www.example.com/3.html', self.parse)


    def parse(self, response):
        yield scrapy.Request(response.url, callback=self.parse_page)
        for page_index in range(2,20):
            next_page_url = re.sub(r'liste_sujet-[0-9]*\.htm$', "liste_sujet-" + `page_index + 1` + ".htm", response.url)
            print "next page parsed: ", next_page_url
            yield scrapy.Request(next_page_url, callback=self.parse_page)

    def parse_page(self, response):
        for topic_href in response.xpath("//a[@class='cCatTopic']/@href"):
            # parse current page
            topic_url = response.urljoin(topic_href.extract())
            yield scrapy.Request(topic_url, callback=self.parse_topic)
            # parse next page
            #next_page_href = response.xpath("//tr[@class='cBackHeader fondForum1PagesHaut']//div[@class='pagination_main_visible']//b/following-sibling::*[1]/@href")
            #next_page_url = response.urljoin(next_page_href.extract())
            #pages_visited.append(next_page_url)
            #yield scrapy.Request(next_page_url, callback=self.parse)
        #pages_visited.sort()
        #print "Pages visited :\n", pages_visited

    def parse_topic(self, response):
        item = DoctissimoItem()
        item['link'] = response.url
        item['title'] = response.xpath("//div[@id='topic']//table[@class='main']//h3/text()").extract()
        # extract()[0] else <value> ... </value> tags would surround the date text
        item['date'] = response.xpath("//div[@id='topic']//table[@class='messagetable'][1]//span[@class='topic_posted']/text()").extract()[0]
        item['message'] = response.xpath("//div[@id='topic']//table[@class='messagetable'][1]//div[@class='post_content']//div/text()").extract()
        yield item
