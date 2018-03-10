package net.ericsson.emovs.cast;


import net.ericsson.emovs.cast.models.EmpCustomData;
import net.ericsson.emovs.cast.models.EmpExposureSettings;
import net.ericsson.emovs.exposure.clients.exposure.ExposureClient;
import net.ericsson.emovs.utilities.emp.EMPRegistry;
import net.ericsson.emovs.utilities.models.EmpAsset;
import net.ericsson.emovs.utilities.models.EmpChannel;
import net.ericsson.emovs.utilities.models.EmpImage;
import net.ericsson.emovs.utilities.models.EmpProgram;
import net.ericsson.emovs.utilities.models.LocalizedMetadata;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/*
 * Copyright (c) 2017 Ericsson. All Rights Reserved
 *
 * This SOURCE CODE FILE, which has been provided by Ericsson as part
 * of an Ericsson software product for use ONLY by licensed users of the
 * product, includes CONFIDENTIAL and PROPRIETARY information of Ericsson.
 *
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS OF
 * THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 */

@RunWith(RobolectricTestRunner.class)
public class CustomDataTest {

    @Test
    public void testExposureSettings() throws Exception {
        EMPRegistry.bindExposureContext("apiUrl1", "dummyCustomer1", "dummyBusinessUnit1");
        ExposureClient.getInstance().setSessionToken("12345");

        JSONObject exposureJson = new EmpExposureSettings().toJson();
        assertEquals("apiUrl1", exposureJson.optString("exposureApiURL"));
        assertEquals("dummyCustomer1", exposureJson.optString("customer"));
        assertEquals("dummyBusinessUnit1", exposureJson.optString("businessUnit"));
        assertEquals("12345", exposureJson.optString("sessionToken"));
    }

    @Test
    public void testCustomData() throws Exception {
        EmpCustomData castProps = new EmpCustomData();
        castProps.setProgramId("program1");
        castProps.absoluteStartTime = 1L;
        castProps.assetId = "channel1";
        castProps.audioLanguage = "pt";
        castProps.autoplay = false;
        castProps.maxBitrate = 10L;
        castProps.startTime = 2L;
        castProps.subtitleLanguage = "sv";
        castProps.timeshiftEnabled = false;
        castProps.useLastViewedOffset = true;

        JSONObject propsJson = castProps.toJson();
        assertEquals("channel1", propsJson.optString("assetId"));
        assertEquals("program1", propsJson.optString("programId"));
        assertEquals("pt", propsJson.optString("audioLanguage"));
        assertEquals("sv", propsJson.optString("textLanguage"));
        assertEquals(true, propsJson.optBoolean("timeShiftDisabled"));
        assertEquals(false, propsJson.optBoolean("autoplay"));
        assertEquals(true, propsJson.optBoolean("useLastViewedOffset"));
        assertEquals(1L, propsJson.optLong("absoluteStartTime"));
        assertEquals(10L, propsJson.optLong("maxBitrate"));
        assertEquals(2, propsJson.optInt("startTime"));
        assertTrue(propsJson.has("ericssonexposure"));
    }

}