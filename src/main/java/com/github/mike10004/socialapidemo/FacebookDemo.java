package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.Reading;
import facebook4j.User;
import facebook4j.internal.org.json.JSONException;

import java.io.PrintStream;

public class FacebookDemo extends Demonstrator<Facebook> {
    public FacebookDemo(Facebook client) {
        super(client);
    }

    @VisibleForTesting
    FacebookDemo(Facebook client, PrintStream output) {
        super(client, output);
    }

    @Override
    public void demonstrate() throws ActivityException {
        try {
            Reading reading = buildReading(USER_FIELDS);
            User user = client.getUser("me", reading);
            output.println(user.getJSON().toString(2));
        } catch (FacebookException | JSONException e) {
            throw new ActivityException(e);
        }
    }

    protected static Reading buildReading(Iterable<String> fieldsToRequest) {
        ImmutableList<String> fields = ImmutableList.copyOf(fieldsToRequest);
        String[] userFields = fields.toArray(new String[fields.size()]);
        Reading reading = new Reading();
        reading.fields(userFields);
        return reading;
    }

    protected static final ImmutableList<String> USER_FIELDS =ImmutableList.of("id",
                "name",
                "about",
                "birthday",
                "currency",
                "devices",
                "education",
                "email",
                "favorite_athletes",
                "favorite_teams",
                "first_name",
                "gender",
                "hometown",
                "inspirational_people",
                "install_type",
                "installed",
                "interested_in",
                "is_verified",
                "languages",
                "last_name",
                "link",
                "location",
                "locale",
                "meeting_for",
                "middle_name",
                "name_format",
                "political",
                "relationship_status",
                "religion",
                "significant_other",
                "third_party_id",
                "quotes",
                "timezone",
                "updated_time",
                "website",
                "verified",
                "work",
                "public_key",
                "cover");

}
