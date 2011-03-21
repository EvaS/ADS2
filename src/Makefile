JCC = javac

JFLAGS = -g

CLASSPATH = .  ../lib

default: two/YahooProber.class two/URLProcessor.class

two/YahooProber.class: two/YahooProber.java  
	$(JCC) $(JFLAGS) -cp .:../lib/json.jar two/YahooProber.java 

two/URLProcessor.class: two/URLProcessor.java
	$(JCC) $(JFLAGS) -cp two/URLProcessor.java 

clean: 
	rm two/*.class
