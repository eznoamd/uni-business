.PHONY: build run clean

build:
	mvn clean package
	docker build -t uni-business:latest .

run:
	docker run uni-business:latest

clean:
	mvn clean
	docker rmi uni-business:latest