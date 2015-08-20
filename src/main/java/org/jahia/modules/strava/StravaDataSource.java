package org.jahia.modules.strava;

import com.google.common.collect.Sets;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Created by Quentin on 14/08/15.
 */
public class StravaDataSource implements ExternalDataSource, ExternalDataSource.Searchable {

    // Strava API
    private static final String API_V3_ATHLETE = "/api/v3/athlete";
    private static final String ACCES_TOKEN    = "access_token";
    private static final String URL_STRAVA     = "www.strava.com";
    private static final Integer PORT_STRAVA   = 443;

    // Node types
    private static final String JNT_STRAVA_ACCOUNT = "jnt:stravaAccount";
    private static final String JNT_STRAVA_SHOES   = "jnt:stravaShoes";
    private static final String JNT_CONTENT_FOLDER = "jnt:contentFolder";

    // Strava key account
    private String apiKeyValue;

    // Http client
    private HttpClient httpClient;

    // Cache
    private EhCacheProvider ehCacheProvider;
    private Ehcache cache;
    private static final String CACHE_NAME            = "strava-cache";
    private static final String CACHE_STRAVA_ACCOUNT  = "cacheStravaAccount";
    
    // Properties : strava
    public static final String ID        = "id";
    public static final String LASTNAME  = "lastname";
    public static final String FIRSTNAME = "firstname";
    public static final String CITY      = "city";
    public static final String STATE     = "state";
    public static final String COUNTRY   = "country";
    public static final String SEX       = "sex";
    public static final String EMAIL     = "email";
    public static final String WEIGHT    = "weight";
    public static final String NAME      = "name";
    public static final String DISTANCE  = "distance";

    // Properties : JCR
    private static final String ROOT  = "root";
    private static final String SHOES = "shoes";
    
    // CONSTRUCTOR

    public StravaDataSource() {
        httpClient = new HttpClient();
    }

    // METHODS

    public void start() {
        // Init method defined in the bean : StravaDataSource
        try {
            if (!ehCacheProvider.getCacheManager().cacheExists(CACHE_NAME)) {
                ehCacheProvider.getCacheManager().addCache(CACHE_NAME);
            }
            cache = ehCacheProvider.getCacheManager().getCache(CACHE_NAME);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (CacheException e) {
            e.printStackTrace();
        }
    }

    // UTILS

    private JSONObject queryStrava(String path, String... params) throws RepositoryException {
        try {
            HttpsURL url = new HttpsURL(URL_STRAVA, PORT_STRAVA, path);
            // Params
            Map<String, String> m = new LinkedHashMap<String, String>();
            m.put(ACCES_TOKEN, apiKeyValue);
            url.setQuery(m.keySet().toArray(new String[m.size()]), m.values().toArray(new String[m.size()]));
            System.out.println("=> Start request strava : " + url);
            GetMethod httpMethod = new GetMethod(url.toString());
            try {
                httpClient.executeMethod(httpMethod);
                cache.put(new Element(CACHE_STRAVA_ACCOUNT, new JSONObject(httpMethod.getResponseBodyAsString())));
                return new JSONObject(httpMethod.getResponseBodyAsString());
            } finally {
                httpMethod.releaseConnection();
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private String getPathForStravaAccount(JSONObject stravaAccount) throws JSONException {
        return "/" + stravaAccount.getString(ID);
    }

    private JSONObject getCacheStravaAccount() throws RepositoryException {
        JSONObject stravaAccount;
        if (cache.get(CACHE_STRAVA_ACCOUNT) != null) {
            stravaAccount = (JSONObject) cache.get(CACHE_STRAVA_ACCOUNT).getObjectValue();
        } else {
            stravaAccount = queryStrava(API_V3_ATHLETE);
        }
        return stravaAccount;
    }

    // IMPLEMENTS : ExternalDataSource

    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            if (identifier.equals(ROOT)) {
                return new ExternalData(identifier, "/", JNT_CONTENT_FOLDER, new HashMap<String, String[]>());
            }
            JSONObject stravaAccount = getCacheStravaAccount();
            Map<String, String[]> properties = new HashMap<String, String[]>();
            // Add all shoes
            if (identifier.contains("shoes")) {
                String[] splitIdentifier = identifier.split("-");
                JSONArray shoes = stravaAccount.getJSONArray(SHOES);
                JSONObject s = (JSONObject) shoes.get(Integer.parseInt(splitIdentifier[1]) - 1);
                properties.put(NAME, new String[]{s.getString(NAME)});
                properties.put(DISTANCE, new String[]{s.getString(DISTANCE)});
                ExternalData data = new ExternalData(identifier, "/" + identifier, JNT_STRAVA_SHOES, properties);
                return data;
            }
            // Add the strava account
            if (stravaAccount.getString(LASTNAME) != null)
                properties.put(LASTNAME, new String[]{stravaAccount.getString(LASTNAME)});
            if (stravaAccount.getString(FIRSTNAME) != null)
                properties.put(FIRSTNAME, new String[]{stravaAccount.getString(FIRSTNAME)});
            if (stravaAccount.getString(CITY) != null)
                properties.put(CITY, new String[]{stravaAccount.getString(CITY)});
            if (stravaAccount.getString(STATE) != null)
                properties.put(STATE, new String[]{stravaAccount.getString(STATE)});
            if (stravaAccount.getString(COUNTRY) != null)
                properties.put(COUNTRY, new String[]{stravaAccount.getString(COUNTRY)});
            if (stravaAccount.getString(SEX) != null)
                properties.put(SEX, new String[]{stravaAccount.getString(SEX)});
            if (stravaAccount.getString(EMAIL) != null)
                properties.put(EMAIL, new String[]{stravaAccount.getString(EMAIL)});
            if (stravaAccount.getString(WEIGHT) != null)
                properties.put(WEIGHT, new String[]{stravaAccount.getString(WEIGHT)});
            ExternalData data = new ExternalData(identifier, "/" + identifier, JNT_STRAVA_ACCOUNT, properties);
            return data;
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }
    }

    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        String[] splitPath = path.split("/");
        try {
            if (splitPath.length <= 1) {
                return getItemByIdentifier(ROOT);
            } else {
                return getItemByIdentifier(splitPath[1]);
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    public List<String> getChildren(String path) throws RepositoryException {
        List<String> r = new ArrayList<String>();
        if (path.equals("/")) {
            try {
                JSONObject stravaAccount = getCacheStravaAccount();
                String pathChild = stravaAccount.getString(ID);
                r.add(pathChild);
                JSONArray shoes = stravaAccount.getJSONArray(SHOES);
                for (int i = 1; i <= shoes.length(); i++) {
                    r.add(SHOES + "-" + i);
                }
            } catch (JSONException e) {
                throw new RepositoryException(e);
            }
        }
        return r;
    }

    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet(JNT_CONTENT_FOLDER, JNT_STRAVA_ACCOUNT, JNT_STRAVA_SHOES);
    }

    public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    public boolean isSupportsUuid() {
        return false;
    }

    public boolean itemExists(String path) {
        return false;
    }

    // Implements : ExternalDataSource.Searchable

    public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        String nodeType = QueryHelper.getNodeType(query.getSource());
        try {
            JSONObject stravaAccount = getCacheStravaAccount();
            if (NodeTypeRegistry.getInstance().getNodeType(JNT_STRAVA_ACCOUNT).isNodeType(nodeType)) {
                String path = getPathForStravaAccount(stravaAccount);
                results.add(path);
            } else if (NodeTypeRegistry.getInstance().getNodeType(JNT_STRAVA_SHOES).isNodeType(nodeType)) {
                JSONArray shoes = stravaAccount.getJSONArray(SHOES);
                for (int i = 1; i <= shoes.length(); i++) {
                    results.add("/" + SHOES + "-" + i);
                }
            }
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
        return results;
    }

    // GETTERS AND SETTERS

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public void setCacheProvider(EhCacheProvider ehCacheProvider) {
        this.ehCacheProvider = ehCacheProvider;
    }

}