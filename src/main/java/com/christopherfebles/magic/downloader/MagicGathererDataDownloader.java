package com.christopherfebles.magic.downloader;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.christopherfebles.magic.model.MagicCardRawData;
import com.christopherfebles.magic.observer.CloneableObserver;

/**
 * Download Magic card data from Gatherer.<br>
 * <br>
 * This class is multi-threaded and uses a recursive method to handle HTTP errors. Downloading thousands of cards will be memory-intensive.<br>
 * <br>
 * NOTE: A null object will be sent to all Observers by each Thread once all cards assigned have been downloaded. This indicates that a downloader Thread has
 * completed.
 * 
 * @author Christopher Febles
 *
 */
public class MagicGathererDataDownloader extends Observable implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger( MagicGathererDataDownloader.class );

    // Constants for loading URLs
    private static final int MAX_ATTEMPTS = 5;
    private static final long WAIT_BEFORE_RETRY_MILLISECONDS = 10 * 1000;

    // True to download text printed on physical card, false for WoTC Oracle text
    private static final boolean GATHERER_PRINTED_TEXT = false;
    private static final String GATHERER_BASE_URL = "http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=" + GATHERER_PRINTED_TEXT + "&multiverseid=";
    private static final String GATHERER_IMAGE_BASE_URL = "http://gatherer.wizards.com/Handlers/Image.ashx?type=card&multiverseid=";
    private static final String GATHERER_LANGUAGE_BASE_URL = "http://gatherer.wizards.com/Pages/Card/Languages.aspx?multiverseid=";

    private Set<CloneableObserver> cloneableObservers;

    /**
     * Default Constructor
     */
    public MagicGathererDataDownloader() {
        cloneableObservers = new HashSet<>();
    }

    @Override
    public MagicGathererDataDownloader clone() {
        LOG.trace( "Cloning a new copy of MagicGathererDataDownloader with id {}.", System.identityHashCode( this ) );
        MagicGathererDataDownloader clone = new MagicGathererDataDownloader();
        LOG.trace( "Created a new copy of MagicGathererDataDownloader with id {}.", System.identityHashCode( clone ) );

        if ( CollectionUtils.isNotEmpty( cloneableObservers ) ) {
            Iterator<CloneableObserver> iterator = cloneableObservers.iterator();
            while ( iterator.hasNext() ) {
                CloneableObserver o = iterator.next();
                clone.addObserver( ( Observer ) o.clone() );
            }
        }

        return clone;
    }

    @Override
    /**
     * Overridden method tracks all Observers watching this object.
     */
    public void addObserver( Observer o ) {
        LOG.trace( "Adding new Observer to MagicGathererDataDownloader with id {}.", System.identityHashCode( this ) );
        super.addObserver( o );
        if ( o instanceof CloneableObserver ) {
            cloneableObservers.add( ( CloneableObserver ) o );
        }
    }

    /**
     * Method to execute as a separate thread
     * 
     * @param idList
     *            The list of IDs to load from Gatherer
     */
    private void run( List<Integer> idList ) {

        long currentThreadId = Thread.currentThread().getId();
        LOG.trace( "New MagicGathererDataDownloader thread {} launched.", currentThreadId );

        // Start main loop
        // Load data from URL
        // Notify observers
        LOG.trace( "Starting update process..." );
        for ( Integer id : idList ) {
            LOG.debug( "Loading Magic Card with Multiverse ID: {}", id );
            String dataUrl = GATHERER_BASE_URL + id;
            String imageUrl = GATHERER_IMAGE_BASE_URL + id;
            String languageUrl = GATHERER_LANGUAGE_BASE_URL + id;

            byte[] dataByteArray = this.loadURL( dataUrl );
            byte[] imageByteArray = this.loadURL( imageUrl );
            byte[] langByteArray = this.loadURL( languageUrl );

            MagicCardRawData data = new MagicCardRawData( id, dataByteArray, imageByteArray, langByteArray );

            this.setChanged();
            // Send newly created object to all observers
            LOG.trace( "Notifying all observers new data object with id {} available for processing.", data.getMultiverseId() );
            this.notifyObservers( data );
        }
        // Using null to indicate this downloader is done processing
        // Using this notification for unit testing
        this.setChanged();
        this.notifyObservers( null );

        // Execution complete
        LOG.trace( "MagicGathererDataDownloader thread {} finished.", currentThreadId );
    }

    /**
     * Download all cards with the Multiverse Ids in the given list.<br>
     * <br>
     * This method parses the given list into blocks of 1000, and launches a separate Thread for each block.
     * 
     * @param idList
     *            The list of multiverse Ids to load
     */
    public void start( List<Integer> idList ) {
        LOG.trace( "New MagicGathererDataDownloader with id {} started.", System.identityHashCode( this ) );

        // For non-threaded operation, call this.run( idList ) directly.

        // Threaded operation
        // Separate idList into groups, and launch a thread for each group
        int partitionSize = 1000;
        List<Integer> partitionedList = new ArrayList<>();

        for ( int x = 0; x < idList.size(); x++ ) {
            partitionedList.add( idList.get( x ) );
            if ( x % partitionSize == 0 && x != 0 ) {
                Thread newThread = new MagicGathererDataDownloaderThread( this.clone(), partitionedList );
                LOG.debug( "New MagicGathererDataDownloaderThread {} launched.", newThread.getId() );
                newThread.start();
                partitionedList = new ArrayList<>();
            }
        }
        // Handle left over Ids
        Thread newThread = new MagicGathererDataDownloaderThread( this.clone(), partitionedList );
        LOG.debug( "Final MagicGathererDataDownloaderThread {} launched.", newThread.getId() );
        newThread.start();

    }

    /**
     * Thread wrapper for a MagicGathererDataDownloader object.<br>
     * <br>
     * Objects of this class will have access to private methods of a given MagicGathererDataDownloader instance.
     * 
     * @author Christopher Febles
     *
     */
    private class MagicGathererDataDownloaderThread extends Thread {

        private MagicGathererDataDownloader downloader;
        private List<Integer> finalIdList;

        /**
         * Create a new Thread to process the given list of Ids with the given downloader.
         * 
         * @param downloader
         *            The object this Thread wraps.
         * @param finalIdList
         *            The list of Ids to process.
         */
        public MagicGathererDataDownloaderThread( MagicGathererDataDownloader downloader, List<Integer> finalIdList ) {
            super();
            this.setDownloader( downloader );
            this.setIdList( finalIdList );
            this.setUncaughtExceptionHandler( new MagicCardThreadLoggingExceptionHandler( LOG ) );
            this.setName( "DL_" + this.getId() );
            this.setDaemon( false );
        }

        private void setIdList( List<Integer> finalIdList ) {
            this.finalIdList = finalIdList;
        }

        private void setDownloader( MagicGathererDataDownloader downloader ) {
            this.downloader = downloader;
        }

        @Override
        /**
         * Calls the run() method on this object's MagicGathererDataDownloader
         */
        public void run() {
            downloader.run( finalIdList );
        }
    }

    /**
     * Load the given URL into a byte array.
     * 
     * @param url
     *            The URL to load
     * @return The response from the URL as a byte array.
     */
    private byte[] loadURL( String url ) {
        return this.loadURLWithCounter( url, 0 );
    }

    /**
     * Recursive method to load the given URLs. In case of network error, this method will recurse {@link #MAX_ATTEMPTS} in an attempt to load the page
     * 
     * @param url
     *            The URL to load
     * @param attemptNumber
     *            The attempt number this is. If a failure occurs, and attemptNumber is less than {@link #MAX_ATTEMPTS}, this method will recurse.
     * @return The response from the URL as a byte array
     */
    private byte[] loadURLWithCounter( String url, int attemptNumber ) {

        byte[] retVal = null;
        int localAttemptCounter = attemptNumber;

        HttpGet httpGet = new HttpGet( url );

        try ( CloseableHttpClient httpClient = HttpClients.createDefault(); 
              CloseableHttpResponse response = httpClient.execute( httpGet ) ) {

            int statusCode = response.getStatusLine().getStatusCode();
            if ( this.checkStatusCode( statusCode ) ) {
                HttpEntity entity = response.getEntity();
                retVal = EntityUtils.toByteArray( entity );

                EntityUtils.consume( entity );
            }
            
        } catch ( SocketException e ) {
            // Assuming a SocketException indicates a network failure
            if ( localAttemptCounter < MAX_ATTEMPTS ) {
                localAttemptCounter++;
                LOG.warn( "Unable to connect to given URL: {}. Retrying... This is attempt number {}.", url, localAttemptCounter, e );
                // Wait before retry
                try {
                    Thread.sleep( WAIT_BEFORE_RETRY_MILLISECONDS );
                } catch ( InterruptedException ie ) {
                    LOG.error( "This should never happen", ie );
                }
                return this.loadURLWithCounter( url, localAttemptCounter );
            } else {
                LOG.error( "Error loading URL: {}. Retried {} times. Giving up.", url, localAttemptCounter, e );
            }
        } catch ( IOException e ) {
            LOG.error( "Error loading URL: {}", url, e );
        }

        return retVal;
    }

    /**
     * Check the if the given status code is {@link HttpStatus#SC_OK}, if not, throw the appropriate exception
     * 
     * @param statusCode        An HTTP status code to check
     * @return  True if the status code is equal to {@link HttpStatus#SC_OK}, otherwise an exception is thrown
     * @throws SocketException  If there is a server-side error. That is, a status code between 500-599, inclusive.
     * @throws IOException      If there is any other error.
     */
    private boolean checkStatusCode( int statusCode ) throws IOException {

        if ( statusCode == HttpStatus.SC_OK ) {
            return true;
        }
        
        if ( statusCode >= 500 && statusCode < 600 ) {
            // For server side errors, retry
            throw new SocketException( "Non-OK Status Code returned: " + statusCode );
        }
        //For all other errors, throw IOException
        throw new IOException( "Non-OK Status Code returned: " + statusCode );
    }

}
