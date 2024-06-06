# TextO

TextO is a library for corpus management and text annotation. It is currently used as one of the backend module of Maia, a web and collaborative tool for text annotation, lexicon editing and lexical linking being developed at CNR-ILC.

TextO allows to upload both raw plain-text documents (UTF-8 encoded), and also documents having an internal, hierarchically organized, logical structure. This can be done by providing the text with a separator at the beginning of each section. The separator must respect the following pattern:

`#+\(type=".*", title=".*", index=".+"\)`

In other words, the separator must start with one or more “#” characters, to indicate the nesting level, followed by the data describing the section with three attributes: a type (e.g. “chapter”),  a title (e.g. “Chapter 1”), and an index, used internally to denote the section. The three attributes can also appear in different order.

Here is an example of text (extracted from the Decameron by Giovanni Boccaccio) marked with a three-level  structure:

`#(type="Giornata", title="Giornata I", index="I")`  
`I`  
`##(type="Novella", title="Novella 1", index="I.1")`  
`I.1`  
`###(type="Paragrafo", title="Paragrafo 1", index="I.1.1")`  
`Ser Cepparello con una falsa confessione inganna un santo frate e muorsi [...]`  
`###(type="Paragrafo", title="Paragrafo 2", index="I.1.2")`  
`Convenevole cosa è, carissime donne, che ciascheduna cosa la quale l’uomo fa [...]`  

## Installation 

### Prerequisites

Before you begin, ensure you have installed the following technologies on your system:

- [NetBeans 22](https://netbeans.apache.org/download/index.html)
- [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- [MySQL 8.0](https://dev.mysql.com/downloads/mysql/8.0.html)

#### MySQL

Create database `texto` with utf8mb4 encoding.  
Execute the JAVA class `it.cnr.ilc.texto.util.DatabaseCreator` to create the initial SQL script and import it into `texto` database.  
Connect to database end change the initial admin password `update _credential set password = upper(sha1('newpassword'));`  

#### JAVA (v.21) jar

In `pom.xml` set:  
  
`<packaging>jar</packaging>`  
  
`<dependency>`  
`     <groupId>org.springframework.boot</groupId>`  
`     <artifactId>spring-boot-starter-tomcat</artifactId>`  
`     <!--<scope>provided</scope>-->`  
`</dependency>`  

In `doc/Install` there are an example of installation directory:  

- `texto.jar` compiled jar file.
- `texto.properties` properties file.  
- `startup.sh` startup script file (must be executable).  
- `shutdown.sh` shutdown script file (must be executable).  
- `springboot.keystore` [keystore](https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl) for Springboot certificate.
- `logs` logs directory

In the properties file, there are only the entries that are intended to be modified compared to the default values. Below are the default values:  

`server.port=7300`  
`server.port-shutdown=7301`  

`server.ssl.key-store=springboot.keystore`  
`server.ssl.key-store-password=keystorepassword`  
`server.ssl.key-store-type=JKS`  
`server.ssl.key-alias=springboot`  
`server.ssl.key-password=password`  

`log.application=logs/application.log`  
`log.sql=logs/sql.log`  

`server.error.include-stacktrace=NEVER`  

`spring.servlet.multipart.max-file-size=16MB`  
`spring.servlet.multipart.max-request-size=16MB`  

`database.url=jdbc:mysql://localhost/texto?characterEncoding=UTF-8&serverTimezone=Europe/Rome`  
`database.username=root`  
`database.password=databasepassword`  
`database.ping=select null`  

`access.session-timeout=1800`  
`access.implementation-class=it.cnr.ilc.texto.manager.access.MaiaAccessImplementation`  

`folder.shared-home=true`  

`resource.default-uploader=plain-text`  

`search.default-width=10`  

#### Tomcat (v.10) war

In `pom.xml` set:  
  
`<packaging>war</packaging>`  
  
`<dependency>`  
`     <groupId>org.springframework.boot</groupId>`  
`     <artifactId>spring-boot-starter-tomcat</artifactId>`  
`     <scope>provided</scope>`  
`</dependency>`  
