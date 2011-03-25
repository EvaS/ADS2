JCC = javac

JFLAGS = -g

CLASSPATH = .  ../lib

default: YahooProber.class URLProcessor.class

YahooProber.class: ./two/YahooProber.java  
	$(JCC) $(JFLAGS) -cp .:../lib/json.jar ./two/YahooProber.java 

URLProcessor.class: ./two/URLProcessor.java
	$(JCC) $(JFLAGS) ./two/URLProcessor.java 

clean: 
	rm two/*.class
