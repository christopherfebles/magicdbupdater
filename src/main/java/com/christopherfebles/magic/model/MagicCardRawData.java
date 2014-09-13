package com.christopherfebles.magic.model;

/**
 * Encapsulate the raw data (HTML pages and card image) downloaded from Gatherer.<br>
 * <br>
 * Instances of this class are used to pass data from MagicGathererDataDownloader to MagicGathererDataObservers.
 * 
 * @see com.christopherfebles.magic.downloader.MagicGathererDataDownloader
 * @see com.christopherfebles.magic.observer.MagicGathererDataObserver
 * 
 * @author Christopher Febles
 *
 */
public class MagicCardRawData {

    private int multiverseId;
    private byte[] dataByteArray;
    private byte[] imageByteArray;
    private byte[] languageByteArray;

    /**
     * Encapsulate the given downloaded data within a new object.
     * 
     * @param multiverseId
     *            The ID of the Magic card to which this data belongs
     * @param dataByteArray
     *            The HTML data downloaded from the card's main Gatherer page
     * @param imageByteArray
     *            The image data dowloaded from Gatherer
     * @param languageByteArray
     *            The HTML data downloaded from the card's Gatherer Language page
     */
    public MagicCardRawData( int multiverseId, byte[] dataByteArray, byte[] imageByteArray, byte[] languageByteArray ) {
        this.setMultiverseId( multiverseId );
        this.setDataByteArray( dataByteArray );
        this.setImageByteArray( imageByteArray );
        this.setLanguageByteArray( languageByteArray );
    }

    /**
     * Get the data (in bytes) downloaded from a Magic card's main Gatherer page.
     * 
     * @return HTML string data, UTF-8 encoded, as a byte array.
     */
    public byte[] getDataByteArray() {
        return dataByteArray;
    }

    public void setDataByteArray( byte[] dataByteArray ) {
        this.dataByteArray = dataByteArray;
    }

    /**
     * Get the card image (in bytes) downloaded from Gatherer.
     * 
     * @return A byte array containing image data
     */
    public byte[] getImageByteArray() {
        return imageByteArray;
    }

    public void setImageByteArray( byte[] imageByteArray ) {
        this.imageByteArray = imageByteArray;
    }

    /**
     * The multiverse ID is a unique identifier of this card data.
     * 
     * @return The multiverse ID of the card this object's data belongs to.
     */
    public int getMultiverseId() {
        return multiverseId;
    }

    public void setMultiverseId( int multiverseId ) {
        this.multiverseId = multiverseId;
    }

    /**
     * Get the data (in bytes) downloaded from a Magic card's Gatherer Languages page.
     * 
     * @return HTML string data, UTF-8 encoded, as a byte array.
     */
    public byte[] getLanguageByteArray() {
        return languageByteArray;
    }

    public void setLanguageByteArray( byte[] languageByteArray ) {
        this.languageByteArray = languageByteArray;
    }

}
