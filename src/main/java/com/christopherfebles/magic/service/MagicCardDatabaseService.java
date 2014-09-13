package com.christopherfebles.magic.service;

import java.util.List;

import com.christopherfebles.magic.dao.MagicCardDAO;

/**
 * Update the Magic Database with the latest values from Gatherer
 * 
 * @author Christopher Febles
 *
 */
public interface MagicCardDatabaseService {

    /**
     * Update/Insert the Magic Card with the given multiverseID in the database
     * 
     * @param multiverseId
     *            The card to update in the database
     */
    void updateMagicDatabase( Integer multiverseId );

    /**
     * Update the existing cards in the database with the newest values from Gatherer.<br>
     * <br>
     * This method will update all cards currently stored in the database.
     * 
     * @see MagicCardDAO#getAllMultiverseIds()
     */
    void updateMagicDatabase();

    /**
     * Populates the database with new, unloaded cards up to {@link #MAXIMUM_MULTIVERSE_ID}<br>
     * <br>
     * Generates a list of all missing IDs in the database up to {@link #MAXIMUM_MULTIVERSE_ID}, and queries Gatherer for them
     */
    void populateMagicDatabase();

    /**
     * Update the existing cards in the database with the newest values from Gatherer.<br>
     * <br>
     * Primary method.
     * 
     * @param idsToUpdate
     *            The list of multiverse Ids to update in the database
     */
    void updateMagicDatabase( List<Integer> idsToUpdate );

}