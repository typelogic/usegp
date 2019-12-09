App.class: App.java
	javac -cp $(shell find libs/ | tr '\n' ':') App.java

test: App.class
	java -cp $(shell find libs/ | tr '\n' ':'):. App

clean:
	@rm -f App.class

.phony: clean
