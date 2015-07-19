package org.jahia.modules.strava;

import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Created by Quentin on 18/07/15.
 */
public class StravaDataSource implements ExternalDataSource, ExternalDataSource.Searchable {

    // ATTRIBUTES

    private static final String API_V3_ATHLETE = "/api/v3/athlete";

    private static final String URL_STRAVA_ACCOUNT = "/stravaAccount/";

    private static final String ACCES_TOKEN = "access_token";

    private static final String URL_STRAVA = "www.strava.com";

    private static final Integer PORT_STRAVA = 443;

    private static final String JNT_STRAVA_ACCOUNT = "jnt:stravaAccount";

    // Strava key account
    private String apiKeyValue;

    // Http client
    private HttpClient httpClient;

    // CONSTRUCTOR

    public StravaDataSource() {
        httpClient = new HttpClient();
    }

    // METHODS

    public void start() {
        // Init method defined in the bean : StravaDataSource
    }

    private JSONObject queryStrava(String path, String... params) throws RepositoryException {
        try {
            HttpsURL url = new HttpsURL(URL_STRAVA, PORT_STRAVA, path);
            // Params
            Map<String, String> m = new LinkedHashMap<String, String>();
            m.put(ACCES_TOKEN, apiKeyValue);
            url.setQuery(m.keySet().toArray(new String[m.size()]), m.values().toArray(new String[m.size()]));
            System.out.println("Start request : " + url);
            GetMethod httpMethod = new GetMethod(url.toString());
            try {
                httpClient.executeMethod(httpMethod);
                return new JSONObject(httpMethod.getResponseBodyAsString());
            } finally {
                httpMethod.releaseConnection();
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private String getPathForStravaAccount(JSONObject stravaAccount) throws JSONException {
        return URL_STRAVA_ACCOUNT + stravaAccount.getString("id");
    }

    // OVERRIDE : ExternalDataSource

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            JSONObject stravaAccount = queryStrava(API_V3_ATHLETE);
            Map<String, String[]> properties = new HashMap<String, String[]>();
            if (stravaAccount.getString("lastname") != null)
                properties.put("lastname", new String[]{stravaAccount.getString("lastname")});
            if (stravaAccount.getString("firstname") != null)
                properties.put("firstname", new String[]{stravaAccount.getString("firstname")});
            if (stravaAccount.getString("city") != null)
                properties.put("city", new String[]{stravaAccount.getString("city")});
            if (stravaAccount.getString("state") != null)
                properties.put("state", new String[]{stravaAccount.getString("state")});
            if (stravaAccount.getString("country") != null)
                properties.put("country", new String[]{stravaAccount.getString("country")});
            if (stravaAccount.getString("sex") != null)
                properties.put("sex", new String[]{stravaAccount.getString("sex")});
            if (stravaAccount.getString("email") != null)
                properties.put("email", new String[]{stravaAccount.getString("email")});
            if (stravaAccount.getString("weight") != null)
                properties.put("weight", new String[]{stravaAccount.getString("weight")});
            ExternalData data = new ExternalData(identifier, getPathForStravaAccount(stravaAccount), JNT_STRAVA_ACCOUNT, properties);
            return data;
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
//        String[] splitPath = path.split("/");
        try {
            return getItemByIdentifier("XXX"); // TODO:
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    // OVERRIDE : ExternalDataSource.Searchable

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        String nodeType = QueryHelper.getNodeType(query.getSource());
        try {
            if (NodeTypeRegistry.getInstance().getNodeType(JNT_STRAVA_ACCOUNT).isNodeType(nodeType)) {
                JSONObject jsonObject = queryStrava(API_V3_ATHLETE);
                String path = getPathForStravaAccount(jsonObject);
                results.add(path);
            }
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
        return results;
    }

    // OVERRIDE : ExternalDataSource

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet("jnt:contentFolder", JNT_STRAVA_ACCOUNT);
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String path) {
        return false;
    }

    // GETTERS AND SETTERS

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

}