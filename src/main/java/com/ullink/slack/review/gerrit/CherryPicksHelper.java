package com.ullink.slack.review.gerrit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CherryPicksHelper
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CherryPicksHelper.class);

    static String getCherryPickLegacyId(String cherryPickRequestJSONResponse)
    {
        LOGGER.debug("parsing Cherry pick request : " + cherryPickRequestJSONResponse);
        cherryPickRequestJSONResponse = cherryPickRequestJSONResponse.substring(4);
        JSONParser parser = new JSONParser();
        long lowestLegacyId = Long.MAX_VALUE;
        try
        {
            JSONArray obj = (JSONArray) parser.parse(cherryPickRequestJSONResponse);
            for (Object jsonChangeObj : obj)
            {
                JSONObject jsonChange = (JSONObject) jsonChangeObj;
                long cherryPickLegacyId = (Long) jsonChange.get("_number");
                if (cherryPickLegacyId < lowestLegacyId)
                {
                    lowestLegacyId = cherryPickLegacyId;
                }
            }
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return Long.toString(lowestLegacyId);
    }
}
