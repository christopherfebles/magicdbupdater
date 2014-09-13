package com.christopherfebles.magic.cli;

import java.io.Console;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.christopherfebles.magic.dao.MagicCardDAO;
import com.christopherfebles.magic.observer.MagicGathererDataObserver;
import com.christopherfebles.magic.service.MagicCardDatabaseService;

/**
 * Command line interface to update the database of Magic cards.
 * 
 * @author Christopher Febles
 *
 */
public class UpdateMagicDatabase {

    private static final Logger LOG = LoggerFactory.getLogger( UpdateMagicDatabase.class );

    @Autowired
    private MagicCardDatabaseService dbUpdaterService;

    @Autowired
    private MagicCardDAO cardDao;

    /**
     * Configure command line options:<br>
     * Options:<br>
     * -updateDatabase<br>
     * -updateWithId {@literal <id>}<br>
     * -updateWithIds {@literal <comma delimited ids>}<br>
     * -populateDatabase<br>
     * 
     * @return  An Options object with all the command line options set.
     */
    private Options setUpCommandLineParameters() {

        Options options = new Options();
        options.addOption( "updateDatabase", false, "Update all Magic cards currently in the database." );
        options.addOption( "updateWithId", true, "Update or Insert the Magic card specified by the given multiverse id." );
        options.addOption( "updateWithIds", true, "Update or Insert the Magic cards specified by the given multiverse ids (comma-separated)." );
        options.addOption( "populateDatabase", false,
                "Insert all Magic cards not currently present in the database starting from the highest existing multiverse id." );

        return options;
    }

    /**
     * Parse the given command line arguments with the given options. Currently all options have at most two values: The option itself, and zero or more Ids.
     * 
     * @see #setUpCommandLineParameters()
     * @param options   The options to parse the given command line with
     * @param arguments The list of command line arguments
     * @return  A Pair consisting of the command line argument (left), and either null or one or more Ids (right) 
     */
    private Pair<String, String> parseCommandLine( Options options, String[] arguments ) {

        String cmdLineArg = null;
        String cmdLineValue = null;
        CommandLineParser parser = new GnuParser();

        try {
            CommandLine line = parser.parse( options, arguments );
            Option[] optionArray = line.getOptions();
            if ( optionArray.length != 1 ) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "UpdateMagicDatabase", options, true );
                throw new ParseException( "Exactly one option required to be present." );
            } else {
                cmdLineArg = optionArray[0].getOpt();
                cmdLineValue = optionArray[0].getValue();
            }
        } catch ( ParseException e ) {
            LOG.error( "Error parsing command line arguments.", e );
        }

        return new ImmutablePair<>( cmdLineArg, cmdLineValue );
    }

    /**
     * Launch the database updater from the command line.<br>
     * Options:<br>
     * -updateDatabase<br>
     * -updateWithId {@literal <id>}<br>
     * -updateWithIds {@literal <comma delimited ids>}<br>
     * -populateDatabase<br>
     * <br>
     * All options except populateDatabase will overwrite existing data in the database. The user will be prompted for confirmation in all overwrite cases.
     * 
     * @param args
     *            Command line arguments
     * @throws InterruptedException
     */
    public static void main( String[] args ) throws InterruptedException {

        UpdateMagicDatabase self = new UpdateMagicDatabase();

        Options options = self.setUpCommandLineParameters();
        Pair<String, String> arguments = self.parseCommandLine( options, args );

        String cmdLineArg = arguments.getLeft();
        String cmdLineValue = arguments.getRight();

        boolean isUpdate = true;
        boolean workActuallyDone = false;
        if ( cmdLineArg != null ) {

            FastDateFormat fdf = FastDateFormat.getInstance( "yyyy-MM-dd HH:mm:ss.SSS" );
            String startDate = fdf.format( new Date() );

            StopWatch timer = new StopWatch();
            timer.start();

            // Manually set up Spring
            self.new ApplicationContextLoader().load( self, "applicationContext.xml" );

            switch ( cmdLineArg ) {
            case "updateDatabase":
                if ( self.confirmOverwriteOption() ) {
                    self.dbUpdaterService.updateMagicDatabase();
                    workActuallyDone = true;
                }
                break;
            case "updateWithId":
                if ( self.confirmOverwriteOption() ) {
                    self.dbUpdaterService.updateMagicDatabase( Integer.parseInt( cmdLineValue ) );
                    workActuallyDone = true;
                }
                break;
            case "updateWithIds":
                workActuallyDone = self.updateWithIds( cmdLineValue );
                break;
            case "populateDatabase":
                isUpdate = false;
                self.dbUpdaterService.populateMagicDatabase();
                workActuallyDone = true;
                break;
            default:
                workActuallyDone = false;
            }

            if ( workActuallyDone ) {
                // Need to wait for accurate timing and count
                self.waitForUpdaterToComplete();

                timer.stop();

                long elapsedTime = timer.getTime();
                LOG.info( "Process begun: {}", startDate );
                LOG.info( self.logStatus( "last request (" + cmdLineArg + ")", elapsedTime ) );
                if ( isUpdate ) {
                    LOG.info( "{} records were written to the database, out of {}.", MagicGathererDataObserver.NUMBER_OF_DATABASE_WRITES.get(),
                            self.cardDao.numberOfCardsInDatabase() );
                } else {
                    LOG.info( "{} records were added to the database.", MagicGathererDataObserver.NUMBER_OF_DATABASE_WRITES.get() );
                }
            }
        }
    }

    /**
     * Update the database with the list of IDs passed in from the command line
     * 
     * @param commaSeparatedIds
     *            A comma separated list of Multiverse Ids
     * @return True if the user confirms this action, false otherwise
     */
    private boolean updateWithIds( String commaSeparatedIds ) {

        boolean workActuallyDone = false;

        if ( this.confirmOverwriteOption() ) {
            String[] idStrAr = commaSeparatedIds.split( "," );
            List<Integer> idList = new ArrayList<>();

            for ( String idStr : idStrAr ) {
                idList.add( Integer.parseInt( idStr ) );
            }

            this.dbUpdaterService.updateMagicDatabase( idList );
            workActuallyDone = true;
        }
        return workActuallyDone;
    }

    /**
     * Confirm a user's selection on the command line which will cause data overwrite
     * 
     * @return True if the user wishes to continue, false otherwise
     */
    private boolean confirmOverwriteOption() {

        Console console = System.console();
        String response = console
                .readLine( "This option will cause card information in the database to be overwritten from Gatherer. Are you sure you want to do this? Y/[N]: " );

        return StringUtils.isNotEmpty( response ) && ( response.trim().equalsIgnoreCase( "Y" ) || response.trim().equalsIgnoreCase( "Yes" ) );
    }

    /**
     * Wait for threads spawned by updater processes to complete.<br>
     * <br>
     * TODO: Update this method to use the Observer pattern, as in MagicCardDatabaseUpdaterTest<br>
     * Not sure if I want to go through the trouble to do this. Maybe in a future refactoring.
     * 
     * @see MagicCardDatabaseUpdaterTest
     * @throws InterruptedException
     */
    private void waitForUpdaterToComplete() throws InterruptedException {

        // Check if threads are still running
        boolean threadsRunning;
        do {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            threadsRunning = false;
            for ( Thread aThread : threadSet ) {
                String threadName = aThread.getName();
                threadsRunning = threadName.startsWith( "DL_" ) || threadName.startsWith( "DO_" );
                if ( threadsRunning ) {
                    LOG.trace( "Thread {} with id {} is still running.", threadName, aThread.getId() );
                    break;
                }
            }
            // Wait a bit, then check again
            Thread.sleep( 1 * 1000 );
        } while ( threadsRunning );
    }

    /**
     * Assemble the "Elapsed Time" log message
     * 
     * @param test
     *            The name of the operation for which we're reporting the elapsed time
     * @param elapsedTime
     *            in milliseconds
     */
    private String logStatus( String test, long elapsedTime ) {

        long elapsedTimeMillis = elapsedTime;

        long hr = TimeUnit.MILLISECONDS.toHours( elapsedTimeMillis );
        long min = TimeUnit.MILLISECONDS.toMinutes( elapsedTimeMillis - TimeUnit.HOURS.toMillis( hr ) );
        long sec = TimeUnit.MILLISECONDS.toSeconds( elapsedTimeMillis - TimeUnit.HOURS.toMillis( hr ) - TimeUnit.MINUTES.toMillis( min ) );
        long ms = TimeUnit.MILLISECONDS.toMillis( elapsedTimeMillis - TimeUnit.HOURS.toMillis( hr ) - TimeUnit.MINUTES.toMillis( min )
                - TimeUnit.SECONDS.toMillis( sec ) );

        return String.format( "Elapsed Time for %s: %02d:%02d:%02d.%03d", test, hr, min, sec, ms );
    }

    /**
     * Bootstraps Spring-managed beans into an application. How to use:
     * <ul>
     * <li>Create application context XML configuration files and put them where they can be loaded as class path resources. The configuration must include the
     * {@code <context:annotation-config/>} element to enable annotation-based configuration, or the {@code <context:component-scan base-package="..."/>}
     * element to also detect bean definitions from annotated classes.
     * <li>Create a "main" class that will receive references to Spring-managed beans. Add the {@code @Autowired} annotation to any properties you want to be
     * injected with beans from the application context.
     * <li>In your application {@code main} method, create an {@link ApplicationContextLoader} instance, and call the {@link #load} method with the "main"
     * object and the configuration file locations as parameters.
     * </ul>
     * 
     * @see <a href="http://stackoverflow.com/questions/4787719/spring-console-application-configured-using-annotations">Stack Overflow</a>
     */
    private class ApplicationContextLoader {

        protected ConfigurableApplicationContext applicationContext;

        public ConfigurableApplicationContext getApplicationContext() {
            return applicationContext;
        }

        /**
         * Loads application context. Override this method to change how the application context is loaded.
         * 
         * @param configLocations
         *            configuration file locations
         */
        protected void loadApplicationContext( String... configLocations ) {
            applicationContext = new ClassPathXmlApplicationContext( configLocations );
            applicationContext.registerShutdownHook();
        }

        /**
         * Injects dependencies into the object. Override this method if you need full control over how dependencies are injected.
         * 
         * @param main
         *            object to inject dependencies into
         */
        protected void injectDependencies( Object main ) {
            getApplicationContext().getBeanFactory().autowireBeanProperties( main, AutowireCapableBeanFactory.AUTOWIRE_NO, false );
        }

        /**
         * Loads application context, then injects dependencies into the object.
         * 
         * @param main
         *            object to inject dependencies into
         * @param configLocations
         *            configuration file locations
         */
        public void load( Object main, String... configLocations ) {
            loadApplicationContext( configLocations );
            injectDependencies( main );
        }
    }
}
