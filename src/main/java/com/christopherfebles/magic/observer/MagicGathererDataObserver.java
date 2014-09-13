package com.christopherfebles.magic.observer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.christopherfebles.magic.dao.MagicCardDAO;
import com.christopherfebles.magic.enums.Color;
import com.christopherfebles.magic.enums.Language;
import com.christopherfebles.magic.model.MagicCard;
import com.christopherfebles.magic.model.MagicCardRawData;

/**
 * Receives downloaded raw data from MagicGathererDataDownloader, converts it to a MagicCard object, and saves to the database.<br>
 * <br>
 * This class' operation is heavily dependent on the format of Gatherer webpages. Should the layout change, this class' behavior could break in unexpected ways.
 * 
 * @author Christopher Febles
 *
 */
public class MagicGathererDataObserver implements CloneableObserver {

    private static final Logger LOG = LoggerFactory.getLogger( MagicGathererDataObserver.class );
    private static final String GATHERER_ID_DEFAULT_PREFIX = "ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_";

    public static final AtomicInteger NUMBER_OF_DATABASE_WRITES = new AtomicInteger();

    private MagicCardDAO cardDAO;
    private String gathererPrefix;

    /**
     * Create a new Observer with a given DAO.<br>
     * <br>
     * Providing the DAO allows this class to operate outside of Spring.
     * 
     * @param cardDAO
     *            The DAO to use to save cards to the database.
     */
    public MagicGathererDataObserver( MagicCardDAO cardDAO ) {
        this.cardDAO = cardDAO;
    }

    @Override
    public MagicGathererDataObserver clone() {
        return new MagicGathererDataObserver( cardDAO );
    }

    @Override
    /**
     * Receive raw data from a currently running downloader and process it.<br>
     * <br>
     * This method is single-threaded, as opposed to earlier versions which kicked off a separate Thread.
     */
    public void update( Observable downloader, Object rawDataObj ) {
        // A null rawDataObj indicates the downloader is 100% complete
        if ( rawDataObj != null ) {
            MagicCardRawData rawData = ( MagicCardRawData ) rawDataObj;
            LOG.trace( "Observer notified of new raw data object with id {} for processing.", rawData.getMultiverseId() );

            // Single-threaded code
            // Data processed immediately
            this.processData( rawData );
        }
    }

    /**
     * Main method of this class:<br>
     * <ul>
     * <li>Calls {@link #parseGathererData(String, int)} to convert the given raw data to a MagicCard.</li>
     * <li>Manually sets image and language data on the MagicCard object.</li>
     * <li>Updates the MagicCard in the database.</li>
     * </ul>
     * 
     * @see #parseGathererData(String, int)
     * 
     * @param rawData
     *            The raw data created by MagicGathererDataDownloader
     */
    private void processData( MagicCardRawData rawData ) {

        String dataStr = new String( rawData.getDataByteArray(), StandardCharsets.UTF_8 );
        MagicCard newCard = this.parseGathererData( dataStr, rawData.getMultiverseId() );

        if ( newCard != null ) {
            LOG.debug( "Setting cardImageArray for MagicCard {}, with ID {}.", newCard.getName(), newCard.getMultiverseId() );
            newCard.setCardImageArray( rawData.getImageByteArray() );

            // Determine language of this card
            String langStr = new String( rawData.getLanguageByteArray(), StandardCharsets.UTF_8 );
            Language cardLang = this.parseLanguagePage( langStr, newCard );
            LOG.debug( "Setting language for MagicCard {}, with ID {} to {}", newCard.getName(), newCard.getMultiverseId(), cardLang );
            newCard.setLanguage( cardLang );

            // Update card stored in database
            LOG.debug( "Saving MagicCard {}, with ID {} to database.", newCard.getName(), newCard.getMultiverseId() );
            boolean success = cardDAO.addCardToDatabase( newCard );
            if ( !success ) {
                LOG.error( "Error saving MagicCard {} with ID {} to database.", newCard.getName(), newCard.getMultiverseId() );
            }
            LOG.trace( "Save of MagicCard {}, with ID {} to database successful. This object has updated the database {} times.", newCard.getName(),
                    newCard.getMultiverseId(), NUMBER_OF_DATABASE_WRITES.incrementAndGet() );
        } else {
            // No card assigned to this id
            LOG.debug( "No card assigned to Multiverse ID: {}", rawData.getMultiverseId() );
        }
    }

    /**
     * Processes the Languages page of a card from Gatherer.
     * 
     * @param languagePage
     *            The raw HTML of the language page from Gatherer
     * @param card
     *            The card for which we're determining a language
     * @return The language of the card, or {@link Language#UNKNOWN_NON_ENGLISH} if it is indeterminate.
     */
    private Language parseLanguagePage( String languagePage, MagicCard card ) {

        Language retVal = Language.ENGLISH;
        List<Language> languageList = new ArrayList<>( Arrays.asList( Language.values() ) );
        languageList.remove( Language.UNKNOWN_NON_ENGLISH );

        for ( Language language : Language.values() ) {
            // Whichever language is NOT present is the language of this card
            if ( languagePage.contains( language.toString() ) ) {
                languageList.remove( language );
            }
        }

        if ( languageList.size() == 1 ) {
            retVal = languageList.get( 0 );
        } else {
            LOG.error( "Ambiguous languages ({}) for card {} with id {}.", languageList, card.getName(), card.getMultiverseId() );
            if ( languageList.contains( Language.ENGLISH ) ) {
                LOG.error( "Setting to ENGLISH as default." );
            } else {
                LOG.error( "Setting to UNKNOWN as default." );
                retVal = Language.UNKNOWN_NON_ENGLISH;
            }
        }

        return retVal;
    }

    /**
     * Convert the given color string into a single character color string.<br>
     * <br>
     * For example, "Black" becomes "B", and "Black or White" becomes "B/W"
     * 
     * @param colorStr
     *            The color string to parse
     * @return The parsed color, or colorStr if a processing error occurs.
     */
    private String parseCardColor( String colorStr ) {

        String localColorStr = colorStr;
        Map<String, Integer> numberMap = new HashMap<>();
        numberMap.put( "ONE", 1 );
        numberMap.put( "TWO", 2 );
        numberMap.put( "THREE", 3 );
        numberMap.put( "FOUR", 4 );
        numberMap.put( "FIVE", 5 );
        numberMap.put( "SIX", 6 );
        numberMap.put( "SEVEN", 7 );
        numberMap.put( "EIGHT", 8 );
        numberMap.put( "NINE", 9 );
        numberMap.put( "TEN", 10 );

        String retVal = "";
        localColorStr = localColorStr.toUpperCase();
        localColorStr = localColorStr.trim().replace( " ", "_" );

        String[] colorAr = null;
        boolean isPhyrexian = false;

        if ( localColorStr.contains( "PHYREXIAN_" ) ) {
            colorAr = localColorStr.split( "PHYREXIAN_" );
            isPhyrexian = true;
        } else {
            colorAr = localColorStr.split( "_OR_" );
        }

        for ( String oneColor : colorAr ) {
            if ( StringUtils.isEmpty( oneColor ) ) {
                continue;
            }
            if ( Color.contains( oneColor ) ) {
                Color color = Color.valueOf( oneColor );
                retVal += color.getValue();
                if ( isPhyrexian ) {
                    retVal += "P";
                }
            } else {
                if ( numberMap.containsKey( oneColor ) ) {
                    retVal += numberMap.get( oneColor );
                } else {
                    retVal += oneColor;
                    // Check for integer here (it should be colorless)
                    if ( !StringUtils.isNumeric( oneColor ) ) {
                        LOG.error( "Unable to load Color {}", oneColor );
                    }
                }
            }

            retVal += "/";
        }
        retVal = retVal.substring( 0, retVal.length() - 1 );

        return retVal;
    }

    /**
     * Get the name of the Magic card from the given Gatherer document
     * 
     * @param gathererDocument
     *            The HTML Gatherer page being parsed
     * @param multiverseId
     *            The ID of the Magic card being parsed
     * @return The name of the Magic card being parsed, or null if an error occurs
     */
    private String getName( Document gathererDocument, int multiverseId ) {

        String prefix = GATHERER_ID_DEFAULT_PREFIX;

        Element nameRow = gathererDocument.select( "[id=" + prefix + "nameRow]" ).first();
        if ( nameRow == null ) {
            for ( int x = 0; x < 10 && nameRow == null; x++ ) {
                prefix = GATHERER_ID_DEFAULT_PREFIX + "ctl0" + x + "_";
                nameRow = gathererDocument.select( "[id=" + prefix + "nameRow]" ).first();
            }
            if ( nameRow == null ) {
                // The given multiverseId doesn't have a card assigned.
                LOG.debug( "Unable to find nameRow for Multiverse ID: {}", multiverseId );
                return null;
            } else {
                // This is a weird kind of card with two values per ID
                LOG.error( "Multiverse ID {} has more than one card assigned to it. Loading the first value.", multiverseId );
            }
        }
        Element cardNameDiv = nameRow.select( "div.value" ).first();

        // This method has a side-effect of determining the correct prefix to use
        gathererPrefix = prefix;

        return cardNameDiv.text();
    }

    /**
     * Get the cost of the Magic card from the given Gatherer document
     * 
     * @param gathererDocument
     *            The HTML Gatherer page being parsed
     * @param multiverseId
     *            The ID of the Magic card being parsed
     * @return The string value of the Mana cost of this card, or null if none exists (such as for lands)
     */
    private String getManaCost( Document gathererDocument, int multiverseId ) {

        String manaCost = null;

        Element manaRow = gathererDocument.select( "[id=" + gathererPrefix + "manaRow]" ).first();
        // Mana cost is optional (for example, for lands)
        if ( manaRow != null ) {
            Element manaDiv = manaRow.select( "div.value" ).first();
            Elements manaImgs = manaDiv.select( "img" );
            manaCost = "";
            Iterator<Element> iterator = manaImgs.iterator();
            while ( iterator.hasNext() ) {
                Element manaImg = iterator.next();
                String color = manaImg.attr( "alt" );
                String colorStr = this.parseCardColor( color );
                manaCost += colorStr + ";";
            }
            // Remove trailing semicolons
            manaCost = manaCost.substring( 0, manaCost.length() - 1 );
        }
        return manaCost;
    }

    /**
     * Get the text of the Magic card from the given Gatherer document
     * 
     * @param gathererDocument
     *            The HTML Gatherer page being parsed
     * @param multiverseId
     *            The ID of the Magic card being parsed
     * @return The text of this card, or null if none exists (such as for lands)
     */
    private String getCardText( Document gathererDocument, int multiverseId ) {

        String cardText = null;

        Element textRow = gathererDocument.select( "[id=" + gathererPrefix + "textRow]" ).first();
        // Text is optional
        if ( textRow != null ) {
            Element cardTextDiv = textRow.select( "div.value" ).first();
            Elements cardTextImgs = cardTextDiv.select( "img" );
            if ( cardTextImgs != null && !cardTextImgs.isEmpty() ) {
                Iterator<Element> imgIterator = cardTextImgs.iterator();
                while ( imgIterator.hasNext() ) {
                    Element img = imgIterator.next();
                    String alt = img.attr( "alt" );

                    if ( alt.equalsIgnoreCase( "tap" ) ) {
                        img.text( "{T}" );
                    } else {
                        if ( StringUtils.isNumeric( alt.trim() ) ) {
                            // Colorless (numeric)
                            img.text( "{" + alt + "}" );
                        } else {
                            // alt is a String
                            String altColor = this.parseCardColor( alt );
                            if ( altColor.equalsIgnoreCase( alt ) ) {
                                LOG.error( "Unexpected image in card text with alt: {} for card with id {}", alt, multiverseId );
                            }
                            img.text( "{" + altColor + "}" );
                        }
                    }
                }
            }
            cardText = cardTextDiv.text();
            LOG.trace( "Setting card text for card with ID {} to: {}", multiverseId, cardText );
        }

        return cardText;
    }

    /**
     * Get the flavor text of the Magic card from the given Gatherer document
     * 
     * @param gathererDocument
     *            The HTML Gatherer page being parsed
     * @param multiverseId
     *            The ID of the Magic card being parsed
     * @return The flavor text of this card, or null of none exists
     */
    private String getFlavorText( Document gathererDocument, int multiverseId ) {

        String flavorText = null;

        Element flavorRow = gathererDocument.select( "[id=" + gathererPrefix + "FlavorText]" ).first();
        // Flavor Text is optional
        if ( flavorRow != null ) {
            Elements flavorTextDiv = flavorRow.select( "div.cardtextbox" );
            // Loop through child divs and insert line breaks
            Iterator<Element> childDivs = flavorTextDiv.iterator();
            flavorText = "";
            while ( childDivs.hasNext() ) {
                Element childDiv = childDivs.next();
                flavorText += childDiv.text() + "\n";
            }
            flavorText = flavorText.trim();
        }

        return flavorText;
    }

    /**
     * Set the power/toughness of the Magic card from the given Gatherer document, if it exists
     * 
     * @param gathererDocument
     *            The HTML Gatherer page being parsed
     * @param card
     *            The MagicCard to assign power/toughness to
     */
    private void setPowerAndToughness( Document gathererDocument, MagicCard card ) {

        Element ptRow = gathererDocument.select( "[id=" + gathererPrefix + "ptRow]" ).first();
        // Power/Toughness is optional
        if ( ptRow != null ) {
            Element ptValueDiv = ptRow.select( "div.value" ).first();
            String ptValueStr = ptValueDiv.text();
            ptValueStr = ptValueStr.replace( "{1/2}", "½" );
            ptValueStr = ptValueStr.replace( "{^2}", "²" );

            if ( ptValueStr.contains( "/" ) ) {
                String[] ptValueAr = ptValueStr.split( "/" );
                card.setPower( ptValueAr[0].trim() );
                card.setToughness( ptValueAr[1].trim() );
            } else {
                // Handle Vanguard
                String[] ptValueAr = ptValueStr.split( "," );
                // Replace non-breaking space with a normal space (regex)
                String power = ptValueAr[0].replaceAll( "\\u00A0", " " ).trim();
                // Get last character of Power String
                card.setPower( String.valueOf( power.toCharArray()[power.length() - 1] ) );

                if ( ptValueAr.length > 1 ) {
                    // Replace non-breaking space with a normal space (regex)
                    String toughness = ptValueAr[1].replaceAll( "\\u00A0", " " ).trim();
                    // Get second to last character of Toughness String
                    card.setToughness( String.valueOf( toughness.toCharArray()[toughness.length() - 2] ) );
                }
            }
        }
    }

    /**
     * Load card data for the given multiverseId into a MagicCard object
     * 
     * @param gathererHtmlPage
     *            The HTML source of the Gatherer page
     * @param multiverseId
     *            The ID used to load the source
     * @return A fully populated MagicCard, or null if no card data exists
     */
    private MagicCard parseGathererData( String gathererHtmlPage, int multiverseId ) {

        LOG.trace( "Parsing raw HTML String card data into MagicCard object for MultiverseId {}.", multiverseId );
        MagicCard newCard = new MagicCard();
        newCard.setMultiverseId( multiverseId );

        Document gathererDocument = Jsoup.parse( gathererHtmlPage );

        // Handle name
        String cardName = this.getName( gathererDocument, multiverseId );
        if ( cardName == null ) {
            // There was an error parsing this card's data
            return null;
        }
        newCard.setName( cardName );

        // Mana Row
        String manaCost = this.getManaCost( gathererDocument, multiverseId );
        if ( manaCost != null ) {
            newCard.setManaCostWithString( manaCost );
        }

        // Skip Converted mana cost, since it's calculated

        // Card Types
        Element typeRow = gathererDocument.select( "[id=" + gathererPrefix + "typeRow]" ).first();
        Element cardTypeDiv = typeRow.select( "div.value" ).first();
        newCard.setTypes( cardTypeDiv.text() );

        // Card text
        String cardText = this.getCardText( gathererDocument, multiverseId );
        if ( cardText != null ) {
            newCard.setText( cardText );
        }

        // Flavor text
        String flavorText = this.getFlavorText( gathererDocument, multiverseId );
        if ( flavorText != null ) {
            newCard.setFlavorText( flavorText );
        }

        // Power/Toughness
        this.setPowerAndToughness( gathererDocument, newCard );

        // Expansion
        Element expansionRow = gathererDocument.select( "[id=" + gathererPrefix + "currentSetSymbol]" ).first();
        Element expansionHref = expansionRow.select( "a" ).last();
        newCard.setExpansion( expansionHref.text() );

        // Rarity
        Element rarityRow = gathererDocument.select( "[id=" + gathererPrefix + "rarityRow]" ).first();
        Element raritySpan = rarityRow.select( "div.value" ).first().select( "span" ).first();
        if ( raritySpan != null ) {
            newCard.setRarity( raritySpan.text() );
        }

        // Card Number
        Element numberRow = gathererDocument.select( "[id=" + gathererPrefix + "numberRow]" ).first();
        // Card Number is optional
        if ( numberRow != null ) {
            Element numberDiv = numberRow.select( "div.value" ).first();
            newCard.setNumber( numberDiv.text() );
        }

        // Artist
        Element artistRow = gathererDocument.select( "[id=" + gathererPrefix + "artistRow]" ).first();
        Element artistHref = artistRow.select( "div.value" ).first().select( "a" ).first();
        if ( artistHref != null ) {
            newCard.setArtist( artistHref.text() );
        } else {
            Element artistDiv = artistRow.select( "div.value" ).first();
            newCard.setArtist( artistDiv.text().trim() );
        }

        // Watermark
        Element markRow = gathererDocument.select( "[id=" + gathererPrefix + "markRow]" ).first();
        // Watermark is optional
        if ( markRow != null ) {
            Element markDiv = markRow.select( "div.value" ).first();
            newCard.setWatermark( markDiv.text() );
        }

        return newCard;
    }
}
