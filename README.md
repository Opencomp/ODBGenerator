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
| __apikey__         | User API Key needed to trigger Opencomp API   |
| __classroom_id__   | Opencomp ```classroom_id``` to retrieve       |

##### Example call
```
http://localhost:8080/ODBGenerator/generateODB?apikey=6400711028fccfd416d7f0f783f9edc28575fe214e5dc60cbe4f230c13cd9158&classroom_id=28
```

#### HTTP responses codes meaning

| HTTP Response                     | Description                                        |
|-----------------------------------|----------------------------------------------------|
| __200 - OK__                      | Browser prompt to download the generated .odb file |
| __401 - Unauthorized__            | Supplied API Key is invalid                        |
| __403 - Forbidden__               | Supplied API Key is valid but you do not have sufficient permission to retrieve this classroom associated data |
| __404 - Not Found__               | ```classroom_id``` supplied does not exist         |
| __405 - Method Not Allowed__      | You need to use GET method                         |
| __412 - Precondition Failed__     | You need to supply all required parameters         |
| __502 - Bad Gateway__             | API did not respond with a valid JSON string       |
| __504 - Gateway Time-out__        | API did not respond within the time limit (15 sec) |
