# Magic the Gathering® Database Updater Project #

### Summary ###

* This project populates and maintains a database of Magic the Gathering® cards.
* Version: 1.0.0

### Build Instructions ###

* Database configuration
    * The project depends on the database being configured according to the [MagicDBAPI](https://github.com/christopherfebles/magicdbapi) project.
* Configuration
    * Spring and Logback properties files are located in src/main/resources/
        * Spring configuration is imported from [MagicDBAPI](https://github.com/christopherfebles/magicdbapi).
    * Maven configuration is split between a parent_pom.xml located in [MagicDBAPI](https://github.com/christopherfebles/magicdbapi) and local, child pom.xml at the root of this project.
* Dependencies
    * Dependencies are loaded by Maven, and documented in the parent and child POM files.
    * Current Dependencies:
        * [MagicDBAPI](https://github.com/christopherfebles/magicdbapi)
        * Spring Context
        * Apache HttpClient
        * Apache Commons
        * Apache CLI
        * JSoup
        * MySQL Connector
        * JUnit
        * Spring Test
        * Apache IO
        * SLF4J
        * Logback
* How to run tests
    * Unit tests now include a mock database, so they can all run independently.
    * Unit tests run as part of the Maven build.
    * An important set of unit tests are located in the MagicGathererDataObserverTest class
        * These tests are all true unit tests.
        * These unit tests test the code's ability to parse and process HTML files downloaded from [Gatherer](http://gatherer.wizards.com/Pages/Default.aspx).
        * If there is ever a problem downloading a specific card or type of card:
            * Load the card in [Gatherer](http://gatherer.wizards.com/Pages/Default.aspx).
            * Save its HTML source to src/test/resources/html
                * The current pattern is to save cards as <multiverseId>.html
            * Write a new Unit Test in MagicGathererDataObserverTest.java to test parsing of the new card
* Deployment instructions
    * The packaged JAR is executable from the command line.
        * A script, MagicDBUpdater.sh, is generated to make command line control easier (for Mac, anyways).
    * Maven build targets:
        * clean package
