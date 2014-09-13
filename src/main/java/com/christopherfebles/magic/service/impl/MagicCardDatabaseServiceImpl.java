package com.christopherfebles.magic.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.christopherfebles.magic.dao.MagicCardDAO;
import com.christopherfebles.magic.downloader.MagicGathererDataDownloader;
import com.christopherfebles.magic.observer.MagicGathererDataObserver;
import com.christopherfebles.magic.service.MagicCardDatabaseService;

/**
 * Update the Magic Database with the latest values from Gatherer
 * 
 * @author Christopher Febles
 *
 */
@Component
public class MagicCardDatabaseServiceImpl implements MagicCardDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger( MagicCardDatabaseServiceImpl.class );
    private static final int MAXIMUM_MULTIVERSE_ID = 500_000;

    @Autowired
    private MagicCardDAO cardDAO;

    /*
     * (non-Javadoc)
     * 
     * @see com.christopherfebles.magic.service.impl.MagicCardDatabaseService#updateMagicDatabase(java.lang.Integer)
     */
    @Override
    public void updateMagicDatabase( Integer multiverseId ) {
        List<Integer> idList = new ArrayList<>();
        idList.add( multiverseId );
        this.updateMagicDatabase( idList );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.christopherfebles.magic.service.impl.MagicCardDatabaseService#updateMagicDatabase()
     */
    @Override
    public void updateMagicDatabase() {
        this.updateMagicDatabase( cardDAO.getAllMultiverseIds() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.christopherfebles.magic.service.impl.MagicCardDatabaseService#populateMagicDatabase()
     */
    @Override
    public void populateMagicDatabase() {

        List<Integer> idList = new ArrayList<>();
        List<Integer> existingIdList = cardDAO.getAllMultiverseIds();

        LOG.debug( "Checking which multiverse ids already exist in the database." );
        for ( int x = 1; x < MAXIMUM_MULTIVERSE_ID; x++ ) {
            if ( !existingIdList.contains( x ) ) {
                idList.add( x );
            }
        }
        LOG.debug( "Preparing to load {} multiverse ids from Gatherer.", idList.size() );

        this.updateMagicDatabase( idList );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.christopherfebles.magic.service.impl.MagicCardDatabaseService#updateMagicDatabase(java.util.List)
     */
    @Override
    public void updateMagicDatabase( List<Integer> idsToUpdate ) {

        LOG.debug( "Creating new Observer and Downloader to update database." );
        MagicGathererDataObserver observer = new MagicGathererDataObserver( cardDAO );
        MagicGathererDataDownloader downloader = new MagicGathererDataDownloader();

        downloader.addObserver( observer );
        LOG.trace( "Registered new MagicGathererDataObserver with downloader." );
        downloader.start( idsToUpdate );
    }
}
