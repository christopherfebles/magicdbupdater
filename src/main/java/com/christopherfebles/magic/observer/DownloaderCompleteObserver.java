package com.christopherfebles.magic.observer;

import java.util.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom observer class that indicates when a MagicGathererDataDownloader is done processing its data.
 * 
 * @author Christopher Febles
 *
 */
public class DownloaderCompleteObserver implements CloneableObserver {

    private static final Logger LOG = LoggerFactory.getLogger( DownloaderCompleteObserver.class );
    private boolean isObserving = true;

    @Override
    public void update( Observable downloader, Object rawDataObj ) {

        LOG.debug( "DownloaderCompleteObserver notified of successful processing. rawDataObj is null? {}", rawDataObj == null );

        // When rawDataObj is null, the downloader is done processing
        if ( rawDataObj == null ) {
            isObserving = false;
        }
    }

    public boolean isObserving() {
        return isObserving;
    }

    @Override
    public DownloaderCompleteObserver clone() {
        return new DownloaderCompleteObserver();
    }
}
