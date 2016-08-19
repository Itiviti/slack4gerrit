package com.ullink.slack.review.gerrit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.GsonHelper;

public class CherryPicksHelper
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CherryPicksHelper.class);

    static String getCherryPickLegacyId(String cherryPickRequestJSONResponse)
    {
        LOGGER.debug("parsing Cherry pick request : " + cherryPickRequestJSONResponse);
        cherryPickRequestJSONResponse = cherryPickRequestJSONResponse.substring(4);
        JsonParser parser = new JsonParser();
        long lowestLegacyId = Long.MAX_VALUE;
        JsonArray obj = parser.parse(cherryPickRequestJSONResponse).getAsJsonArray();
        for (JsonElement jsonElement : obj)
        {
            JsonObject jsonChange = jsonElement.getAsJsonObject();
            long cherryPickLegacyId = GsonHelper.getLongOrNull(jsonChange.get("_number"));
            if (cherryPickLegacyId < lowestLegacyId)
            {
                lowestLegacyId = cherryPickLegacyId;
            }
        }
        return Long.toString(lowestLegacyId);
    }
}
