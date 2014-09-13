package com.christopherfebles.magic.test;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.christopherfebles.magic.downloader.MagicGathererDataDownloader;
import com.christopherfebles.magic.testsupport.IntegrationTest;
import com.christopherfebles.magic.testsupport.UnitTest;

@PrepareForTest( { MagicGathererDataDownloader.class } )
public class MagicGathererDataDownloaderTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    
    private static final Logger LOG = LoggerFactory.getLogger( MagicGathererDataDownloaderTest.class );
    private static final int ANKH_OF_MISHRA_ID = 1;
    
    private MagicGathererDataDownloader downloader;
    
    @Before
    public void setUp() {
        downloader = new MagicGathererDataDownloader();
    }
    
    @Test
    @Category( UnitTest.class )
    public void testIdListPartitioner() throws Exception {

        //Ids are partitioned in groups of 1000. This will test correct separation and leftovers
        int numberOfIds = 10_635;
        
        //Test that a given list is partitioned correctly
        final List<Integer> idsAssignedToThreads = new ArrayList<>();
        
        //Mock objects        
        Class<Object> magicGathererDataDownloaderThread = Whitebox.getInnerClassType( MagicGathererDataDownloader.class, "MagicGathererDataDownloaderThread" );
        PowerMockito.replace( PowerMockito.method( magicGathererDataDownloaderThread, List.class ) )
            .with( new InvocationHandler() {
    
                @SuppressWarnings( "unchecked" )
                @Override
                public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
                    
                    LOG.trace( "Called replaced method for MagicGathererDataDownloaderThread.setIdList()" );
                    assertNotNull( args );
                    assertTrue( args.length == 1 );
                    assertTrue( args[0] instanceof List );
                    
                    List<Integer> idList = (List<Integer>)args[0];
                    assertTrue( idList.size() <= 1001 );
                    idsAssignedToThreads.addAll( idList );
                    return null;
                }
                
            });
        PowerMockito.replace( PowerMockito.method( magicGathererDataDownloaderThread, "start" ) )
            .with( new InvocationHandler() {
    
                @Override
                public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
                    LOG.trace( "Called replaced method for MagicGathererDataDownloaderThread.start()" );
                    return null;
                }
                
            });
        
        //Generate ID list
        List<Integer> idList = new ArrayList<>();
        for ( int x = 1; x < numberOfIds; x++ ) {
            idList.add( x );
        }
        
        downloader.start( idList );
        
        Collections.sort( idsAssignedToThreads );
        //Test that all Ids passed to the downloader are assigned to threads for processing
        assertEquals( idList, idsAssignedToThreads );
    }
    
    @Test
    @Category( IntegrationTest.class )
    public void testLoadUrlWithImageData() throws Exception {
        
        String imgURL = Whitebox.getInternalState( MagicGathererDataDownloader.class, "GATHERER_IMAGE_BASE_URL" );

        String url = imgURL + ANKH_OF_MISHRA_ID;
        byte[] imageByteArray = Whitebox.invokeMethod( downloader, "loadURL", url );
        
        assertNotNull( imageByteArray );
        assertTrue( imageByteArray.length > 0 );
        
        //Test if valid image downloaded
        ByteArrayInputStream bais = new ByteArrayInputStream( imageByteArray );
        BufferedImage cardImage = ImageIO.read( bais );
        
        assertNotNull( cardImage );
    }
    
    @Test
    @Category( IntegrationTest.class )
    public void testLoadUrlWithStringData() throws Exception {
        
        String baseURL = Whitebox.getInternalState( MagicGathererDataDownloader.class, "GATHERER_BASE_URL" );
        
        String url = baseURL + ANKH_OF_MISHRA_ID;
        byte[] dataByteArray = Whitebox.invokeMethod( downloader, "loadURL", url );
        
        assertNotNull( dataByteArray );
        assertTrue( dataByteArray.length > 0 );
        
        //Test if valid string downloaded
        String dataStr = new String( dataByteArray, StandardCharsets.UTF_8 );
        
        assertNotNull( dataStr );
        assertTrue( dataStr.length() > 0 );
        assertTrue( dataStr.contains( "alt=\"Ankh of Mishra\"" ) );
    }

}
