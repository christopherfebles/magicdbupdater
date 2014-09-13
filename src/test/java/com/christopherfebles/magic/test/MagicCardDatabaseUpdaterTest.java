package com.christopherfebles.magic.test;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.christopherfebles.magic.downloader.MagicGathererDataDownloader;
import com.christopherfebles.magic.enums.Language;
import com.christopherfebles.magic.enums.Type;
import com.christopherfebles.magic.model.MagicCard;
import com.christopherfebles.magic.observer.DownloaderCompleteObserver;
import com.christopherfebles.magic.service.MagicCardDatabaseService;
import com.christopherfebles.magic.service.impl.MagicCardDatabaseServiceImpl;
import com.christopherfebles.magic.testsupport.DAOTester;
import com.christopherfebles.magic.testsupport.UnitTest;

/**
 * This class contains integration tests of MagicCardDatabaseUpdater
 * 
 * @author Christopher Febles
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/applicationContext-test.xml" } )
@PrepareForTest( { MagicGathererDataDownloader.class, MagicCardDatabaseServiceImpl.class } )
@Category( UnitTest.class )
public class MagicCardDatabaseUpdaterTest extends DAOTester {

    private static final Logger LOG = LoggerFactory.getLogger( MagicCardDatabaseUpdaterTest.class );
    private static final int ANKH_OF_MISHRA_ID = 1;

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    
    @Autowired
    private MagicCardDatabaseService dbUpdater;
    
    private MagicGathererDataDownloader setUpDownloaderSpy() throws Exception {

        MagicGathererDataDownloader mockedDownloader = PowerMockito.spy( new MagicGathererDataDownloader() );
        
        //PowerMock convenience methods for Java Reflection
        String baseURL = Whitebox.getInternalState( MagicGathererDataDownloader.class, "GATHERER_BASE_URL" );
        String imgURL = Whitebox.getInternalState( MagicGathererDataDownloader.class, "GATHERER_IMAGE_BASE_URL" );
        String langURL = Whitebox.getInternalState( MagicGathererDataDownloader.class, "GATHERER_LANGUAGE_BASE_URL" );

        //Load return results
        String filename = "html/1.html";
        InputStream is = MagicCardDatabaseUpdaterTest.class.getClassLoader().getResourceAsStream( filename );
        byte[] dataByteArray = IOUtils.toByteArray( is );
        filename = "html/1_language.html";
        is = MagicCardDatabaseUpdaterTest.class.getClassLoader().getResourceAsStream( filename );
        byte[] langByteArray = IOUtils.toByteArray( is );
        filename = "html/1.jpg";
        is = MagicCardDatabaseUpdaterTest.class.getClassLoader().getResourceAsStream( filename );
        byte[] imgByteArray = IOUtils.toByteArray( is );
        
        //Set up mocked downloader
        PowerMockito.doReturn( dataByteArray ).when( mockedDownloader, "loadURL", Mockito.startsWith( baseURL ) );
        PowerMockito.doReturn( imgByteArray ).when( mockedDownloader, "loadURL", Mockito.startsWith( imgURL ) );
        PowerMockito.doReturn( langByteArray ).when( mockedDownloader, "loadURL", Mockito.startsWith( langURL ) );
        
        //Fix cloning error
        //Since I'm mocking this object, new() returns the same mocked object. 
        //    This causes a ConcurrentModificationException in clone(), but only in test. 
        PowerMockito.doReturn( mockedDownloader ).when( mockedDownloader, "clone" );
        
        PowerMockito.whenNew( MagicGathererDataDownloader.class ).withNoArguments().thenReturn( mockedDownloader );
        
        return mockedDownloader;
    }

    @Test
    public void testUpdateOfSingleCard() throws Exception {
        
        MagicGathererDataDownloader mockedDownloader = this.setUpDownloaderSpy();
        //Add observer to know when downloader is done processing
        DownloaderCompleteObserver observer = new DownloaderCompleteObserver();
        mockedDownloader.addObserver( observer );
        
        String customFlavor = "Custom awesome flavor text.";
        
        MagicCard originalAnkh = cardDAO.getCardFromDatabaseById( ANKH_OF_MISHRA_ID );
        assertNotNull( originalAnkh );
        
        assertNull( originalAnkh.getFlavorText() );
        originalAnkh.setFlavorText( customFlavor );
        cardDAO.saveCardToDatabase( originalAnkh );
        
        originalAnkh = cardDAO.getCardFromDatabaseById( ANKH_OF_MISHRA_ID );
        assertNotNull( originalAnkh.getFlavorText() );
        assertEquals( customFlavor, originalAnkh.getFlavorText() );
        
        dbUpdater.updateMagicDatabase( ANKH_OF_MISHRA_ID );
        //Wait for updater to be done processing
        while ( observer.isObserving() ) {
            Thread.sleep( 1000 );
        }
        
        MagicCard newAnkh = cardDAO.getCardFromDatabaseById( ANKH_OF_MISHRA_ID );
        assertNull( newAnkh.getFlavorText() );
    }
    
    @Test
    public void testUpdateAllCurrentCards() throws Exception {
        
        //Check list of all current cards, and ensure that the correct list is being sent to the update method
        //The update method itself is tested as part of testUpdateOfSingleCard()
        final List<Integer> expectedList = cardDAO.getAllMultiverseIds();
        
        PowerMockito.replace( PowerMockito.method( MagicCardDatabaseServiceImpl.class, "updateMagicDatabase", List.class ) )
            .with( new InvocationHandler() {

                @Override
                public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {

                    List<Integer> localExpectedList = expectedList; 
                    assertNotNull( args );
                    assertTrue( args.length > 0 );
                    assertTrue( args[0] instanceof List );
                    assertEquals( localExpectedList, args[0] );
                    return null;
                }
                
            });
        
        dbUpdater.updateMagicDatabase();
    }
    
    protected static boolean databaseUpdaterTesterInitializationComplete = false;
    @Override
    public void additionalSetUp() {
        
        if ( !databaseUpdaterTesterInitializationComplete ) {
            LOG.trace( "Initializing embedded database for MagicCardDatabaseUpdater tests." );
            
            MagicCard ankhOfMishra = new MagicCard( ANKH_OF_MISHRA_ID, "Ankh of Mishra", "2", Type.ARTIFACT.toString(), "Limited Edition Alpha" );
            ankhOfMishra.setLanguage( Language.ENGLISH );
            
            //Insert into database
            assertTrue( cardDAO.addCardToDatabase( ankhOfMishra ) );
        }
    }

}
