package com.christopherfebles.magic.powermock;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.christopherfebles.magic.testsupport.UnitTest;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/applicationContext-test.xml" } )
@MockPolicy( Slf4jMockPolicy.class )
@Category( UnitTest.class )
/**
 * Test the configuration of PowerMock within this project
 * 
 * @author Christopher Febles
 *
 */
public class PowerMockTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testConfiguration() {
        assertTrue( true );
    }

}
