.PHONY: clean make
all: 
	mvn clean package

clean:
	rm -rf *.o
	rm -rf file.s Quad file
	rm -rf target
