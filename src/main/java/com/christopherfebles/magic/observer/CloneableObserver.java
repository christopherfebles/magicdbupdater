package com.christopherfebles.magic.observer;

import java.util.Observer;

/**
 * An interface that identifies Observers which should be cloned along with MagicGathererDataDownloader.
 * 
 * @see com.christopherfebles.magic.downloader.MagicGathererDataDownloader
 * @author Christopher Febles
 *
 */
public interface CloneableObserver extends Observer, Cloneable {

    /**
     * Override Object's clone() method.<br>
     * <br>
     * This ensures that classes can clone based on just this interface.
     * 
     * @return A clone of this object.
     */
    Object clone();

}
