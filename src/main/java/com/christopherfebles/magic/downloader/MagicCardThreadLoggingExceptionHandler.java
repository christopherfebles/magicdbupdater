package com.christopherfebles.magic.downloader;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles (that is, logs) exceptions thrown within a running Thread. Otherwise, such exceptions would be lost.
 * 
 * @author Christopher Febles
 *
 */
public class MagicCardThreadLoggingExceptionHandler implements UncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger( MagicCardThreadLoggingExceptionHandler.class );

    private Logger logger;

    /**
     * Set the logger to write unhandled exception logs to.<br>
     * <br>
     * If logger is null, this class will write unhandled exceptions to its own logger. This may (depending on logging configuration) make it appear as if
     * messages originated from this class.
     * 
     * @param logger
     *            Logger object, may be null.
     */
    public MagicCardThreadLoggingExceptionHandler( Logger logger ) {
        this.logger = logger;
    }

    @Override
    public void uncaughtException( Thread t, Throwable e ) {
        // Thread names now include id number.
        if ( logger != null ) {
            logger.error( "Exception in running thread named {}", t.getName(), e );
        } else {
            LOG.error( "Exception in running thread named {}", t.getName(), e );
        }
    }
}
