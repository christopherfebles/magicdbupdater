package com.christopherfebles.magic.test;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.christopherfebles.magic.dao.MagicCardDAO;
import com.christopherfebles.magic.enums.CardType;
import com.christopherfebles.magic.enums.Color;
import com.christopherfebles.magic.enums.Language;
import com.christopherfebles.magic.enums.SuperType;
import com.christopherfebles.magic.enums.Type;
import com.christopherfebles.magic.model.MagicCard;
import com.christopherfebles.magic.model.Mana;
import com.christopherfebles.magic.observer.MagicGathererDataObserver;
import com.christopherfebles.magic.testsupport.UnitTest;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/applicationContext-test.xml" } )
@Category( UnitTest.class )
public class MagicGathererDataObserverTest {

    private static final int ANKH_OF_MISHRA_ID = 1;
    private static final int ANKH_OF_MISHRA_VINTAGE_ID = 382844;
    private static final int ARCHANGEL_ID = 6514;
    private static final int ISLAND_ID = 383283;
    private static final int BAZAAR_OF_BAGHDAD_ID = 984;
    private static final int UNASSIGNED_MULTIVERSE_ID = 3756;
    private static final int BARRIN_VANGUARD_ID = 4957;
    private static final int CLOCKWORK_BEAST_ID = 7;
    private static final int WILD_WURM_ID = 4856;
    private static final int JACE_PLANESWALKER_ID = 211170;
    private static final int PINE_BARRENS_ID = 383048;
    private static final int MIDNIGHT_RECOVERY_ID = 366476;
    private static final int SLAUGHTERHORN_ID = 367775;
    private static final int AVATAR_OF_DISCORD_ID = 107437;
    private static final int SPECTRAL_PROCESSION_ID = 172092;
    private static final int URBAN_EVOLUTION_ID = 368160;
    private static final int ACT_OF_AGGRESSION_ID = 230076;
    private static final int SHIELD_OF_KALDRA_ID = 97055;
    private static final int DRAIN_LIFE_ID = 61;
    
    @SuppressWarnings( "unused" )
    private static final int BLINDING_SOULEATER_ID = 233045;//Colorless with Phyrexian White active ability
    
    //Language tests
    private static final int BLASTFIRE_BOLT_ENGLISH_ID = 383192;
    private static final int BLASTFIRE_BOLT_FRENCH_ID = 384328;
    private static final int ALTARS_LIGHT_JAPANESE_ID = 73602;
    
    //Transforming cards
    private static final int HUNTMASTER_OF_THE_FELLS_ID = 262875;
    private static final int TAKE_ID = 369097;

    @Autowired
    private MagicCardDAO cardDAO;
    
    private MagicGathererDataObserver observer;
    
    @Before
    public void setUp() {
        observer = new MagicGathererDataObserver( cardDAO );
    }
    
    @Test
    public void testTransformingCard_Huntmaster() throws Exception {
        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( HUNTMASTER_OF_THE_FELLS_ID + ".html" ), HUNTMASTER_OF_THE_FELLS_ID );
        assertNotNull( card );
        
        assertEquals( 4, card.getConvertedCost().intValue() );
        assertEquals( "140a", card.getNumber() );
        assertEquals( "Huntmaster of the Fells".toLowerCase(), card.getName().toLowerCase() );
    }
    
    @Test
    public void testTransformingCard_Take() throws Exception {
        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( TAKE_ID + ".html" ), TAKE_ID );
        assertNotNull( card );
        
        assertEquals( 3, card.getConvertedCost().intValue() );
        assertEquals( "129a", card.getNumber() );
        assertEquals( "Take".toLowerCase(), card.getName().toLowerCase() );
    }
    
    @Test
    public void testCardColorWithVariableColorless() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( DRAIN_LIFE_ID + ".html" ), DRAIN_LIFE_ID );
        assertNotNull( card );
        
        assertFalse( card.getColors().contains( Color.COLORLESS ) );
        assertFalse( card.getColors().contains( Color.VARIABLE_COLORLESS ) );
    }
    
    @Test
    public void testLoadCardNoRarity() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( SHIELD_OF_KALDRA_ID + ".html" ), SHIELD_OF_KALDRA_ID );
        assertNotNull( card );
        
        assertNull( card.getRarity() );
    }
    
    @Test
    public void testLoadPhyrexianCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ACT_OF_AGGRESSION_ID + ".html" ), ACT_OF_AGGRESSION_ID );
        assertNotNull( card );
        
        assertEquals( "R", card.getColorsString() );
        assertEquals( 1, card.getColors().size() );
        assertEquals( Color.RED, card.getColors().get( 0 ) );

        assertEquals( 5, card.getConvertedCost().intValue() );
        assertEquals( "3RPRP", card.getManaCostString() );
    }
    
    @Test
    public void testLoadUrbanEvolution() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( URBAN_EVOLUTION_ID + ".html" ), URBAN_EVOLUTION_ID );
        assertNotNull( card );
        
        assertEquals( "GU", card.getColorsString() );
        assertEquals( 2, card.getColors().size() );
        assertEquals( Color.GREEN, card.getColors().get( 0 ) );
        assertEquals( Color.BLUE, card.getColors().get( 1 ) );

        assertEquals( 5, card.getConvertedCost().intValue() );
        assertEquals( "3GU", card.getManaCostString() );
    }
    
    @Test
    public void testLoadColorlessCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ANKH_OF_MISHRA_ID + ".html" ), ANKH_OF_MISHRA_ID );
        assertNotNull( card );
        
        assertEquals( "C", card.getColorsString() );
        assertEquals( 1, card.getColors().size() );
        assertEquals( Color.COLORLESS, card.getColors().get( 0 ) );

        assertEquals( 2, card.getConvertedCost().intValue() );
        assertEquals( "2", card.getManaCostString() );
    }
    
    @Test
    public void testLoadMulticolorCardWithColorless() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( SPECTRAL_PROCESSION_ID + ".html" ), SPECTRAL_PROCESSION_ID );
        assertNotNull( card );
        
        assertEquals( "W", card.getColorsString() );
        assertEquals( 1, card.getColors().size() );
        assertEquals( Color.WHITE, card.getColors().get( 0 ) );

        assertEquals( 6, card.getConvertedCost().intValue() );
        assertEquals( "{2/W}{2/W}{2/W}", card.getManaCostString() );
    }
    
    @Test
    public void testLoadMulticolorCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( AVATAR_OF_DISCORD_ID + ".html" ), AVATAR_OF_DISCORD_ID );
        assertNotNull( card );
        
        assertEquals( "BR", card.getColorsString() );
        assertTrue( card.getColors().contains( Color.BLACK ) );
        assertTrue( card.getColors().contains( Color.RED ) );
        
        String cardText = "({B/R} can be paid with either {B} or {R}.) Flying When Avatar of Discord enters the battlefield, sacrifice it unless you discard two cards.";
        assertEquals( cardText, card.getText() );
    }
    
    @Test
    public void testLoadMulticolorCardCost() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( AVATAR_OF_DISCORD_ID + ".html" ), AVATAR_OF_DISCORD_ID );
        assertNotNull( card );
        
        assertEquals( 3, card.getConvertedCost().intValue() );
        
        List<Mana> cost = card.getManaCost();
        
        assertEquals( 3, cost.size() );
        assertTrue( cost.get(0).getColors().contains( Color.BLACK ) &&
                cost.get(0).getColors().contains( Color.RED ));
        assertTrue( cost.get(1).getColors().contains( Color.BLACK ) &&
                cost.get(1).getColors().contains( Color.RED ));
        assertTrue( cost.get(2).getColors().contains( Color.BLACK ) &&
                cost.get(2).getColors().contains( Color.RED ));
    }
    
    @Test
    public void testNullPointerExceptionInLanguage() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( SLAUGHTERHORN_ID + ".html" ), SLAUGHTERHORN_ID );
        assertNotNull( card );
        
        Language language = this.parseLanguagePageFromGathererWithCard( 
                this.loadResourceFileAsString( SLAUGHTERHORN_ID + "_russian.html" ), card );
        
        assertEquals( Language.RUSSIAN, language );
    }
    
    @Test
    public void testLoadUnknownCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ALTARS_LIGHT_JAPANESE_ID + ".html" ), ALTARS_LIGHT_JAPANESE_ID );
        assertNotNull( card );
        
        Language language = this.parseLanguagePageFromGathererWithCard( 
                this.loadResourceFileAsString( ALTARS_LIGHT_JAPANESE_ID + "_japanese.html" ), card );
        
        assertEquals( Language.UNKNOWN_NON_ENGLISH, language );
    }
    
    @Test
    public void testLoadEnglishCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( BLASTFIRE_BOLT_ENGLISH_ID + ".html" ), BLASTFIRE_BOLT_ENGLISH_ID );
        assertNotNull( card );
        
        Language language = this.parseLanguagePageFromGathererWithCard( 
                this.loadResourceFileAsString( BLASTFIRE_BOLT_ENGLISH_ID + "_english.html" ), card );
        
        assertEquals( Language.ENGLISH, language );
    }
    
    @Test
    public void testLoadFrenchCard() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( BLASTFIRE_BOLT_FRENCH_ID + ".html" ), BLASTFIRE_BOLT_FRENCH_ID );
        assertNotNull( card );
        
        Language language = this.parseLanguagePageFromGathererWithCard( 
                this.loadResourceFileAsString( BLASTFIRE_BOLT_FRENCH_ID + "_french.html" ), card );
        
        assertEquals( Language.FRENCH, language );
    }
    
    @Test
    public void testLoadCardWithWatermark() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( MIDNIGHT_RECOVERY_ID + ".html" ), MIDNIGHT_RECOVERY_ID );
        
        assertNotNull( card );
        assertEquals( "Dimir", card.getWatermark() );
    }
    
    @Test
    public void testLoadCardNotSaving() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( PINE_BARRENS_ID + ".html" ), PINE_BARRENS_ID );
        
        assertNotNull( card );
        assertEquals( "307", card.getNumber() );
    }
    
    @Test
    public void testLoadCardWhenPlaneswalker() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( JACE_PLANESWALKER_ID + ".html" ), JACE_PLANESWALKER_ID );
        
        assertNotNull( card );
        assertEquals( "3", card.getPower() );
        assertEquals( "3", card.getLoyalty() );
    }
    
    @Test
    public void testLoadCardWithMultilineFlavorText() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( WILD_WURM_ID + ".html" ), WILD_WURM_ID );
        
        assertNotNull( card );
        assertNotNull( card.getFlavorText() );
        assertTrue( card.getFlavorText().length() > 0 );
        assertTrue( card.getFlavorText().contains( "\n" ) );
    }
    
    @Test
    public void testLoadCardWithTapAbility() throws Exception {
        
        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( CLOCKWORK_BEAST_ID + ".html" ), CLOCKWORK_BEAST_ID );
        
        assertNotNull( card );
        
        String cardText = "Clockwork Beast enters the battlefield with seven +1/+0 counters on it. "
                + "At end of combat, if Clockwork Beast attacked or blocked this combat, remove a "
                + "+1/+0 counter from it. {X}, {T}: Put up to X +1/+0 counters on Clockwork Beast. "
                + "This ability can't cause the total number of +1/+0 counters on Clockwork Beast to "
                + "be greater than seven. Activate this ability only during your upkeep.";

        assertEquals( cardText, card.getText() );
    }
    
    @Test
    public void testLoadVanguardCard() throws Exception {
        
        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( BARRIN_VANGUARD_ID + ".html" ), BARRIN_VANGUARD_ID );
        
        assertNotNull( card );
        assertEquals( "0", card.getPower() );
        assertEquals( "6", card.getToughness() );
    }

    @Test
    public void testParseGathererDataWithoutArtist() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( BAZAAR_OF_BAGHDAD_ID + ".html" ), BAZAAR_OF_BAGHDAD_ID );
        
        assertNotNull( card );
        assertEquals( BAZAAR_OF_BAGHDAD_ID, card.getMultiverseId().intValue() );
        assertEquals( "(none)", card.getArtist().toLowerCase() );
    }

    @Test
    public void testParseGathererDataWithoutFlavorText() throws Exception {

        MagicCard ankhOfMishra = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ANKH_OF_MISHRA_ID + ".html" ), ANKH_OF_MISHRA_ID );
        
        assertNotNull( ankhOfMishra );
        assertEquals( ANKH_OF_MISHRA_ID, ankhOfMishra.getMultiverseId().intValue() );
        assertEquals( Type.ARTIFACT.toString().toLowerCase(), ankhOfMishra.getType().toLowerCase() );
    }

    @Test
    public void testParseGathererDataWithFlavorText() throws Exception {

        MagicCard ankhOfMishra = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ANKH_OF_MISHRA_VINTAGE_ID + ".html" ), ANKH_OF_MISHRA_VINTAGE_ID );
        
        assertNotNull( ankhOfMishra );
        assertEquals( ANKH_OF_MISHRA_VINTAGE_ID, ankhOfMishra.getMultiverseId().intValue() );
        assertEquals( Type.ARTIFACT.toString().toLowerCase(), ankhOfMishra.getType().toLowerCase() );
        assertNotNull( ankhOfMishra.getFlavorText() );
        assertTrue( ankhOfMishra.getFlavorText().length() > 0 );
    }

    @Test
    public void testParseGathererDataWithCardNumber() throws Exception {

        MagicCard ankhOfMishra = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ANKH_OF_MISHRA_VINTAGE_ID + ".html" ), ANKH_OF_MISHRA_VINTAGE_ID );
        
        assertNotNull( ankhOfMishra );
        assertEquals( ANKH_OF_MISHRA_VINTAGE_ID, ankhOfMishra.getMultiverseId().intValue() );
        assertEquals( Type.ARTIFACT.toString().toLowerCase(), ankhOfMishra.getType().toLowerCase() );
        assertNotNull( ankhOfMishra.getFlavorText() );
        assertEquals( "263", ankhOfMishra.getNumber() );
    }

    @Test
    public void testParseGathererDataWithManaCost() throws Exception {
        
        MagicCard archangel = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ARCHANGEL_ID + ".html" ), ARCHANGEL_ID );
        
        assertNotNull( archangel );
        assertEquals( ARCHANGEL_ID, archangel.getMultiverseId().intValue() );
        assertEquals( ( Type.CREATURE.toString() + CardType.TYPE_SEPARATOR_WITH_SPACES + "Angel" ).toLowerCase(), archangel.getType().toLowerCase() );
        assertEquals( 7, archangel.getConvertedCost().intValue() );
        
        assertTrue( archangel.getColors().contains( Color.WHITE ) );
        assertEquals( 1, archangel.getColors().size() );
        assertEquals( ( "5WW" ).toLowerCase(), archangel.getManaCostString().toLowerCase() );
    }

    @Test
    public void testParseGathererDataWithLand() throws Exception {
        
        MagicCard island = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( ISLAND_ID + ".html" ), ISLAND_ID );
        
        assertNotNull( island );
        assertEquals( ISLAND_ID, island.getMultiverseId().intValue() );
        assertEquals( ( SuperType.BASIC.toString() + " " + Type.LAND.toString() + CardType.TYPE_SEPARATOR_WITH_SPACES + "Island" ).toLowerCase(), island.getType().toLowerCase() );
        assertEquals( 0, island.getConvertedCost().intValue() );
        assertTrue( island.getColors().contains( Color.COLORLESS ) );
        assertEquals( "0", island.getManaCostString() );
    }

    @Test
    public void testLoadGathererDataURLWithInvalidId() throws Exception {

        MagicCard card = this.parseMagicCardFromGathererWithId( 
                this.loadResourceFileAsString( UNASSIGNED_MULTIVERSE_ID + ".html" ), UNASSIGNED_MULTIVERSE_ID );
        
        assertNull( card );
    }

    /**
     * Parse the given HTML String containing card data from Gatherer for a card with the given ID
     * 
     * @param webpageStr    The full HTML String from Gatherer
     * @param id            The ID of the card to extract
     * @return                A fully populated Magic card
     * @throws Exception    If an exception occurs
     */
    private MagicCard parseMagicCardFromGathererWithId( String webpageStr, int id ) throws Exception {
        return Whitebox.invokeMethod( observer, "parseGathererData", webpageStr, id );
    }
    
    private Language parseLanguagePageFromGathererWithCard( String webpageStr, MagicCard card ) throws Exception {
        return Whitebox.invokeMethod( observer, "parseLanguagePage", webpageStr, card );
    }
    
    private String loadResourceFileAsString( String filename ) throws Exception {

        filename = "html/" + filename;
        InputStream is = MagicGathererDataObserverTest.class.getClassLoader().getResourceAsStream( filename );

        return IOUtils.toString( is, StandardCharsets.UTF_8 );
    }
    
}
