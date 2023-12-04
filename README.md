# Upload Files

Create a web server in your local machine to share files between your home network.

## Pre Requisites
- jdk 17
- mvn

### Build
```shell
mvn clean install
```

### Run
```shell
mvn spring-boot:run
```

### Enjoy

http://localhost:8080

## Docker

### Create image
```shell
docker build --tag upload-files .
```

### Tag image
```shell
docker tag upload-files:latest upload-files:v1.0.0
```

### Run image in container
```shell
docker run -d -p 8080:8080 --name upload-files-server upload-files
```