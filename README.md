# ODBGenerator
Java servlet to generate OpenDocument Database file from Opencomp API

## Purpose

This servlet is used by Opencomp web application to provide a way to automatically generate an OpenDocument Database file containing the pupils related to a classroom.

Informations are retrieved via Opencomp API.

## Usage

### Configure API Endpoint

You need to define the address of the main Opencomp instance by filling ```apiURl param-value``` into ```init-param``` section of ```web.xml``` file.

### Call servlet /ODBGenerator/generateODB?```<params>```

#### GET parameters required

| Parameter          | Description                                   |
|--------------------|-----------------------------------------------|
| __ apikey __       | User API Key needed to trigger Opencomp API   |
| __ classroom_id__  | Opencomp ```classroom_id``` to retrieve       |

##### Example call
```
http://localhost:8080/ODBGenerator/generateODB?apikey=6400711028fccfd416d7f0f783f9edc28575fe214e5dc60cbe4f230c13cd9158&classroom_id=28
```

#### HTTP responses codes meaning

| HTTP Response                     | Description                                   |
|-----------------------------------|-----------------------------------------------|
| __ 200 - OK __                    | Your browser should prompt to download the generated .odb file                                                                 |
| __ 405 - Method Not Allowed __    | You need to use GET method                    |
| __ 412 - Precondition Failed __   | You need to supply all required parameters    |
| __ 401 - Unauthorized __          | The supplied API Key is invalid or you do not have sufficient permission to retrieve this classroom associated data               |
| __ 404 - Not Found __             | ```classroom_id``` you specified does not exist    |
