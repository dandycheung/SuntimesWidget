/**
    Copyright (C) 2022 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.calculator;

import android.content.Context;

import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;

import org.junit.Test;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SuntimesDescriptorTest
{
    @Test
    public void test_initCalculators()
    {
        SuntimesCalculatorDescriptor.initCalculators((Context)null);
        SuntimesCalculatorDescriptor[] values0 = SuntimesCalculatorDescriptor.values((Context)null);
        assertNotNull(values0);
        assertEquals(6, values0.length);

        ArrayList<SuntimesCalculatorDescriptor> descriptors0 = new ArrayList<>(Arrays.asList(values0));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.getDescriptor()));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.ca.rmen.sunrisesunset.SunriseSunsetSuntimesCalculator.getDescriptor()));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.time4a.Time4ASimpleSuntimesCalculator.getDescriptor()));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.time4a.Time4ANOAASuntimesCalculator.getDescriptor()));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.time4a.Time4ACCSuntimesCalculator.getDescriptor()));
        assertTrue(descriptors0.contains(com.forrestguice.suntimeswidget.calculator.time4a.Time4A4JSuntimesCalculator.getDescriptor()));
    }

    @Test
    public void test_reinitCalculators()
    {
        SuntimesCalculatorDescriptor[] values0 = SuntimesCalculatorDescriptor.values((Context)null);    // values() calls initCalculators if uninitialized
        assertNotNull(values0);
        assertEquals(6, values0.length);

        SuntimesCalculatorDescriptor.removeValue(com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.getDescriptor());
        SuntimesCalculatorDescriptor[] values1 = SuntimesCalculatorDescriptor.values((Context)null);
        assertEquals(5, values1.length);

        SuntimesCalculatorDescriptor.reinitCalculators((Context)null);
        SuntimesCalculatorDescriptor[] values3 = SuntimesCalculatorDescriptor.values((Context)null);
        assertNotNull(values3);
        assertEquals(6, values3.length);
    }

    @Test
    public void test_addRemove()
    {
        assertEquals(6, SuntimesCalculatorDescriptor.values((Context)null).length);
        SuntimesCalculatorDescriptor.removeValue(com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.getDescriptor());
        assertEquals(5, SuntimesCalculatorDescriptor.values((Context)null).length);

        SuntimesCalculatorDescriptor.addValue(com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.getDescriptor());
        assertEquals(6, SuntimesCalculatorDescriptor.values((Context)null).length);

        SuntimesCalculatorDescriptor.addValue(com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.getDescriptor());  // was already added
        assertEquals(6, SuntimesCalculatorDescriptor.values((Context)null).length);
    }

    @Test
    public void test_valueOf()
    {
        boolean caughtException0 = false;
        try {
            SuntimesCalculatorDescriptor.valueOf((Context)null, "dne");
        } catch (InvalidParameterException e) {
            caughtException0 = true;
        }
        assertTrue(caughtException0);

        boolean caughtException1 = false;
        SuntimesCalculatorDescriptor descriptor1 = null;
        try {
            descriptor1 = SuntimesCalculatorDescriptor.valueOf((Context)null, "any");   // special value 'any'
        } catch (InvalidParameterException e) {
            caughtException1 = true;
        }
        assertFalse(caughtException1);
        assertNotNull(descriptor1);
    }

    @Test
    public void test_SuntimesCalculatorDescriptor_new()
    {
        SuntimesCalculatorDescriptor descriptor0 = new SuntimesCalculatorDescriptor("name", "display", "classRef");
        assertEquals("name", descriptor0.getName());
        assertEquals(descriptor0.getName(), descriptor0.toString());
        assertEquals("display", descriptor0.getDisplayString());
        assertEquals("classRef", descriptor0.getReference());
        assertEquals(-1, descriptor0.getDisplayStringResID());
        assertTrue(descriptor0.hasRequestedFeature(SuntimesCalculator.FEATURE_RISESET));
        assertFalse(descriptor0.isPlugin());

        descriptor0.setIsPlugin(true);
        assertTrue(descriptor0.isPlugin());

        SuntimesCalculatorDescriptor descriptor1 = new SuntimesCalculatorDescriptor("name", "display", "classRef", 5);
        assertEquals("name", descriptor1.getName());
        assertEquals("display", descriptor1.getDisplayString());
        assertEquals("classRef", descriptor1.getReference());
        assertEquals(5, descriptor1.getDisplayStringResID());
        assertTrue(descriptor1.hasRequestedFeature(SuntimesCalculator.FEATURE_RISESET));
        assertFalse(descriptor1.isPlugin());

        SuntimesCalculatorDescriptor descriptor2 = new SuntimesCalculatorDescriptor("name", "display", "classRef", 5, new int[] { /* empty features */});
        assertEquals("name", descriptor2.getName());
        assertEquals("display", descriptor2.getDisplayString());
        assertEquals("classRef", descriptor2.getReference());
        assertEquals(5, descriptor1.getDisplayStringResID());
        assertFalse(descriptor2.hasRequestedFeature(SuntimesCalculator.FEATURE_RISESET));
        assertEquals(0, descriptor2.getSupportedFeatures().length);
        assertFalse(descriptor2.isPlugin());
    }

}
