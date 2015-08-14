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
    private static final String ACCES_TOKEN = "access_token";
    private static final String URL_STRAVA = "www.strava.com";
    private static final Integer PORT_STRAVA = 443;

    // Node types
    private static final String JNT_STRAVA_ACCOUNT = "jnt:stravaAccount";
    private static final String JNT_CONTENT_FOLDER = "jnt:contentFolder";

    // Strava key account
    private String apiKeyValue;

    // Http client
    private HttpClient httpClient;

    // Cache
    private EhCacheProvider ehCacheProvider;
    private Ehcache cache;
    private static final String CACHE_NAME = "strava-cache";
    private static final String CACHE_STRAVA_ACCOUNT  = "cacheStravaAccount";

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
        return "/" + stravaAccount.getString("id");
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

    // OVERRIDE : ExternalDataSource

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            if (identifier.equals("root")) {
                return new ExternalData(identifier, "/", "jnt:contentFolder", new HashMap<String, String[]>());
            }
            JSONObject stravaAccount = getCacheStravaAccount();
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
            ExternalData data = new ExternalData(identifier, "/" + identifier, JNT_STRAVA_ACCOUNT, properties);
            return data;
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        String[] splitPath = path.split("/");
        try {
            if (splitPath.length <= 1) {
                return getItemByIdentifier("root");
            } else {
                return getItemByIdentifier(splitPath[1]);
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    // OVERRIDE : ExternalDataSource

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        List<String> r = new ArrayList<String>();
        if (path.equals("/")) {
            try {
                JSONObject stravaAccount = getCacheStravaAccount();
                String pathChild = stravaAccount.getString("id");
                r.add(pathChild);
            } catch (JSONException e) {
                throw new RepositoryException(e);
            }
        }
        return r;
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet(JNT_CONTENT_FOLDER, JNT_STRAVA_ACCOUNT);
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

    // OVERRIDE : ExternalDataSource.Searchable

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        String nodeType = QueryHelper.getNodeType(query.getSource());
        try {
            if (NodeTypeRegistry.getInstance().getNodeType(JNT_STRAVA_ACCOUNT).isNodeType(nodeType)) {
                JSONObject stravaAccount = getCacheStravaAccount();
                String path = getPathForStravaAccount(stravaAccount);
                results.add(path);
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