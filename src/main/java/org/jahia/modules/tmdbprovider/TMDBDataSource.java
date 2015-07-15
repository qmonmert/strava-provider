package org.jahia.modules.tmdbprovider;

import com.google.common.collect.Sets;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.*;
import java.util.*;
import java.util.regex.Pattern;

public class TMDBDataSource implements ExternalDataSource, ExternalDataSource.Searchable, ExternalDataSource.LazyProperty {
    public static final HashSet<String> LAZY_PROPERTIES = Sets.newHashSet("original_title", "homepage", "status", "runtime", "imdb_id", "budget", "revenue");
    public static final HashSet<String> LAZY_I18N_PROPERTIES = Sets.newHashSet("jcr:title", "overview", "tagline", "poster_path");

    public static final HashSet<String> ROOT_NODES = Sets.newHashSet("movies", "lists", "persons");

    private static String API_URL = "api.themoviedb.org";
    private static String API_CONFIGURATION = "/3/configuration";
    private static String API_MOVIE = "/3/movie/";
    private static String API_TV = "/3/tv/";
    private static String API_DISCOVER_MOVIE = "/3/discover/movie";
    private static String API_DISCOVER_TV = "/3/discover/tv";
    private static String API_SEARCH_MOVIE = "/3/search/movie";
    private static String API_KEY = "api_key";

    private static String API_FIND = "/3/find/";

    private static Pattern YEAR_PATTERN = Pattern.compile("[0-9]{4,4}");
    private static Pattern DATE_PATTERN = Pattern.compile("[0-9]{4,4}/[0-9]{2,2}");

    private EhCacheProvider ehCacheProvider;
    private Ehcache cache;
    private String apiKeyValue;

    private String accountId;
    private String token;
    private String sessionId;

    private HttpClient httpClient;

    public TMDBDataSource() {
        httpClient = new HttpClient();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setCacheProvider(EhCacheProvider ehCacheProvider) {
        this.ehCacheProvider = ehCacheProvider;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public void start() {
        try {
            if (!ehCacheProvider.getCacheManager().cacheExists("tmdb-cache")) {
                ehCacheProvider.getCacheManager().addCache("tmdb-cache");
            }
            cache = ehCacheProvider.getCacheManager().getCache("tmdb-cache");
        } catch (IllegalStateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CacheException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * @param path path where to get children
     * @return list of paths as String
     */
    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        List<String> r = new ArrayList<String>();

        String[] splitPath = path.split("/");
        try {
            if (splitPath.length == 0) {
                r.addAll(ROOT_NODES);
                return r;
            } else if (splitPath[1].equals("movies")) {
                switch (splitPath.length) {
                    case 2:
                        for (int i = 1900; i <= 2013; i++) {
                            r.add(Integer.toString(i));
                        }
                        return r;
                    case 3:
                        for (int i = 1; i <= 12; i++) {
                            r.add(StringUtils.leftPad(Integer.toString(i), 2, "0"));
                        }
                        return r;
                    case 4:
                        final String date = splitPath[2] + "-" + splitPath[3];
                        if (cache.get("movies-folder-" + date) != null) {
                            r = (List<String>) cache.get("movies-folder-" + date).getObjectValue();
                        } else {
                            JSONObject o = queryTMDB(API_DISCOVER_MOVIE, "release_date.gte", date + "-01", "release_date.lte", splitPath[2] + "-" + splitPath[3] + "-31");
                            JSONArray result = o.getJSONArray("results");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject movie = result.getJSONObject(i);
                                r.add(movie.getString("id"));
                                cache.put(new Element("movie-" + movie.getString("id"), movie.toString()));
                            }
                            cache.put(new Element("movies-folder-" + date, r));
                        }
                        return r;
                    case 5:
                        JSONObject o;
                        if (cache.get("movies-credits-" + splitPath[4]) != null) {
                            o = new JSONObject((String) cache.get("movies-credits-" + splitPath[4]).getObjectValue());
                        } else {
                            o = queryTMDB("/3/movie/" + splitPath[4] + "/credits");
                            cache.put(new Element("movies-credits-" + splitPath[4], o.toString()));
                        }
                        JSONArray result = o.getJSONArray("cast");
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject cast = result.getJSONObject(i);
                            r.add("cast_" + cast.getString("cast_id") + "_" + cast.getString("id"));
                        }
                        result = o.getJSONArray("crew");
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject crew = result.getJSONObject(i);
                            if (!r.contains("crew_" + crew.getString("id"))) {
                                r.add("crew_" + crew.getString("job") + "_" + crew.getString("id"));
                            }
                        }
                        return r;
                }
            } else if (splitPath[1].equals("lists")) {
                switch (splitPath.length) {
                    case 2:
                        if (cache.get("lists") != null) {
                            r = (List<String>) cache.get("lists").getObjectValue();
                        } else {
                            JSONObject o = queryTMDB("/3/account/" + getAccountId() + "/lists", "session_id", getSessionId());
                            JSONArray result = o.getJSONArray("results");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject list = result.getJSONObject(i);
                                r.add(list.getString("id"));
                                cache.put(new Element("list-" + list.getString("id"), list.toString()));
                            }
                            cache.put(new Element("lists", r));
                        }
                        return r;
                    case 3:
                        JSONObject list;
                        if (cache.get("fulllist-" + splitPath[2]) != null) {
                            list = new JSONObject((String) cache.get("fulllist-" + splitPath[2]).getObjectValue());
                        } else {
                            list = queryTMDB("/3/list/" + splitPath[2]);
                            cache.put(new Element("fulllist-" + splitPath[2], list.toString()));
                        }
                        JSONArray result = list.getJSONArray("items");
                        for (int i = 0; i < Math.min(result.length(),20); i++) {
                            JSONObject movie = result.getJSONObject(i);
                            r.add(movie.getString("id"));
                            cache.put(new Element("movieref-" + movie.getString("id"), movie.toString()));
                        }
                        return r;
                }
            } else if (splitPath[1].equals("persons")) {
                switch (splitPath.length) {
                    case 2:
                        return r;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    /**
     * identifier is unique for an ExternalData
     *
     * @param identifier
     * @return ExternalData defined by the identifier
     * @throws javax.jcr.ItemNotFoundException
     *
     */
    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            if (identifier.equals("root")) {
                return new ExternalData(identifier, "/", "jnt:contentFolder", new HashMap<String, String[]>());
            }
            if (identifier.contains("-rootfolder")) {
                final String s = StringUtils.substringBefore(identifier, "-rootfolder");
                if (ROOT_NODES.contains(s)) {
                    return new ExternalData(identifier, "/" + s, "jnt:contentFolder", new HashMap<String, String[]>());
                }
            } else if (identifier.contains("-folder-")) {
                final String s = StringUtils.substringBefore(identifier, "-folder-");
                final String date = StringUtils.substringAfter(identifier, "-folder-");
                if (ROOT_NODES.contains(s) && (YEAR_PATTERN.matcher(date).matches() || DATE_PATTERN.matcher(date).matches())) {
                    return new ExternalData(identifier, "/" + s + "/" + date, "jnt:contentFolder", new HashMap<String, String[]>());
                }
            } else if (identifier.startsWith("movie-")) {
                String movieId = StringUtils.substringAfter(identifier, "movie-");
                try {
                    Integer.parseInt(movieId);
                } catch (NumberFormatException e) {
                    throw new ItemNotFoundException(identifier);
                }
                JSONObject movie;

                String lang = "en";

                if (cache.get("movie-" + movieId) != null) {
                    movie = new JSONObject((String) cache.get("movie-" + movieId).getObjectValue());
                } else if (cache.get("fullmovie-" + lang + "-" + movieId) != null) {
                    movie = new JSONObject((String) cache.get("fullmovie-" + lang + "-" + movieId).getObjectValue());
                } else {
                    movie = queryTMDB(API_MOVIE + movieId, "language", lang);
                    cache.put(new Element("fullmovie-" + lang + "-" + movieId, movie.toString()));
                }

                JSONObject configuration = getConfiguration();
                String baseUrl = configuration.getJSONObject("images").getString("base_url");

                Map<String, String[]> properties = new HashMap<String, String[]>();
                if (!StringUtils.isEmpty(movie.getString("backdrop_path")) && !movie.getString("backdrop_path").equals("null"))
                    properties.put("backdrop_path", new String[]{baseUrl + configuration.getJSONObject("images").getJSONArray("backdrop_sizes").get(1) + movie.getString("backdrop_path")});
                if (movie.getString("release_date") != null)
                    properties.put("release_date", new String[]{movie.getString("release_date") + "T00:00:00.000+00:00"});
                if (movie.getString("adult") != null) properties.put("adult", new String[]{movie.getString("adult")});
                if (movie.getString("vote_average") != null)
                    properties.put("vote_average", new String[]{movie.getString("vote_average")});
                if (movie.getString("vote_count") != null)
                    properties.put("vote_count", new String[]{movie.getString("vote_count")});
                if (movie.getString("popularity") != null)
                    properties.put("popularity", new String[]{movie.getString("popularity")});

                ExternalData data = new ExternalData(identifier, getPathForMovie(movie), "jnt:movie", properties);

                data.setLazyProperties(new HashSet<String>(LAZY_PROPERTIES));

                Map<String, Set<String>> lazy18 = new HashMap<String, Set<String>>();
                lazy18.put("en", new HashSet<String>(LAZY_I18N_PROPERTIES));
                lazy18.put("fr", new HashSet<String>(LAZY_I18N_PROPERTIES));
                data.setLazyI18nProperties(lazy18);

                return data;
            } else if (identifier.startsWith("moviecredits-")) {
                String movieId = StringUtils.substringAfter(identifier, "moviecredits-");
                String creditsId = StringUtils.substringAfter(movieId, "-");
                movieId = StringUtils.substringBefore(movieId, "-");

                ExternalData movie = getItemByIdentifier("movie-" + movieId);

                JSONObject o;
                if (cache.get("movies-credits-" + movieId) != null) {
                    o = new JSONObject((String) cache.get("movies-credits-" + movieId).getObjectValue());
                } else {
                    o = queryTMDB("/3/movie/" + movieId + "/credits");
                    cache.put(new Element("movies-credits-" + movieId, o.toString()));
                }

                Map<String, String[]> properties = new HashMap<String, String[]>();

                JSONObject configuration = getConfiguration();
                String baseUrl = configuration.getJSONObject("images").getString("base_url");

                JSONObject credit = null;
                ExternalData data = null;
                if (creditsId.startsWith("crew_")) {
                    String id = StringUtils.substringAfter(creditsId, "crew_");
                    String job = StringUtils.substringBefore(id,"_");
                    id = StringUtils.substringAfter(id,"_");
                    JSONArray a = o.getJSONArray("crew");
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject crew = a.getJSONObject(i);
                        if (crew.getString("id").equals(id) && crew.getString("job").equals(job)) {
                            data = new ExternalData(identifier, movie.getPath() + "/" + creditsId, "jnt:crew", properties);
                            credit = crew;
                            if (credit.getString("department") != null)
                                properties.put("department", new String[]{credit.getString("department")});
                            if (credit.getString("job") != null)
                                properties.put("job", new String[]{credit.getString("job")});
                            if (credit.getString("id") != null)
                                properties.put("person", new String[]{"person-"+credit.getString("id")});
                            break;
                        }
                    }
                } else if (creditsId.startsWith("cast_")) {
                    String id = StringUtils.substringAfter(creditsId, "cast_");
                    String castId = StringUtils.substringBefore(id,"_");
                    id = StringUtils.substringAfter(id,"_");
                    JSONArray a = o.getJSONArray("cast");
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject cast = a.getJSONObject(i);
                        if (cast.getString("id").equals(id) && cast.getString("cast_id").equals(castId)) {
                            data = new ExternalData(identifier, movie.getPath() + "/" + creditsId, "jnt:cast", properties);
                            credit = cast;
                            if (credit.getString("character") != null)
                                properties.put("character", new String[]{credit.getString("character")});
                            if (credit.getString("order") != null)
                                properties.put("order", new String[]{credit.getString("order")});
                            if (credit.getString("cast_id") != null)
                                properties.put("cast_id", new String[]{credit.getString("cast_id")});
                            if (credit.getString("id") != null)
                                properties.put("person", new String[]{"person-"+credit.getString("id")});
                            break;
                        }
                    }
                }
                if (credit != null) {
                    if (credit.getString("name") != null)
                        properties.put("name", new String[]{credit.getString("name")});
                    if (!StringUtils.isEmpty(credit.getString("profile_path")) && !credit.getString("profile_path").equals("null"))
                        properties.put("profile", new String[]{baseUrl + configuration.getJSONObject("images").getJSONArray("profile_sizes").get(1) + credit.getString("profile_path")});


                    return data;
                }
            } else if (identifier.startsWith("lists-")) {
                String listId = StringUtils.substringAfter(identifier, "lists-");
                if (!Pattern.compile("[a-z0-9]+").matcher(listId).matches()) {
                    throw new ItemNotFoundException(identifier);
                }

                JSONObject list;

                if (cache.get("list-" + listId) != null) {
                    list = new JSONObject((String) cache.get("list-" + listId).getObjectValue());
                } else if (cache.get("fulllist-" + listId) != null) {
                    list = new JSONObject((String) cache.get("fulllist-" + listId).getObjectValue());
                } else {
                    list = queryTMDB("/3/list/" + listId);
                    cache.put(new Element("fulllist-" + listId, list.toString()));
                }

                JSONObject configuration = getConfiguration();
                String baseUrl = configuration.getJSONObject("images").getString("base_url");

                Map<String, String[]> properties = new HashMap<String, String[]>();
                if (list.getString("name") != null)
                    properties.put("jcr:title", new String[]{list.getString("name")});
                if (list.getString("description") != null)
                    properties.put("jcr:description", new String[]{list.getString("description")});
                if (!StringUtils.isEmpty(list.getString("poster_path")) && !list.getString("poster_path").equals("null"))
                    properties.put("poster_path", new String[]{baseUrl + configuration.getJSONObject("images").getJSONArray("poster_sizes").get(1) + list.getString("poster_path")});

                ExternalData data = new ExternalData(identifier, "/lists/" + listId, "jnt:moviesList", properties);
                return data;
            } else if (identifier.startsWith("movieref-")) {
                String movieId = StringUtils.substringAfter(identifier, "movieref-");
                String listId = StringUtils.substringBefore(movieId, "-");
                movieId = StringUtils.substringAfter(movieId, "-");
                try {
                    Integer.parseInt(movieId);
                } catch (NumberFormatException e) {
                    throw new ItemNotFoundException(identifier);
                }
                if (!Pattern.compile("[a-z0-9]+").matcher(listId).matches()) {
                    throw new ItemNotFoundException(identifier);
                }

                Map<String, String[]> properties = new HashMap<String, String[]>();
                properties.put("j:node", new String[]{"movie-" + movieId});
                ExternalData data = new ExternalData(identifier, "/lists/" + listId + "/" + movieId, "jnt:contentReference", properties);
                return data;
            } else if (identifier.startsWith("person-")) {
                String personId = StringUtils.substringAfter(identifier, "person-");
                try {
                    Integer.parseInt(personId);
                } catch (NumberFormatException e) {
                    throw new ItemNotFoundException(identifier);
                }
                JSONObject person;
                if (cache.get("person-" + personId) != null) {
                    person = new JSONObject((String) cache.get("person-" + personId).getObjectValue());
                } else {
                    person = queryTMDB("/3/person/" + personId);
                    cache.put(new Element("person-" + personId, person.toString()));
                }

                JSONObject configuration = getConfiguration();
                String baseUrl = configuration.getJSONObject("images").getString("base_url");

                Map<String, String[]> properties = new HashMap<String, String[]>();
                if (person.getString("name") != null)
                    properties.put("name", new String[]{person.getString("name")});
                if (person.getString("biography") != null)
                    properties.put("biography", new String[]{person.getString("biography")});
                if (!StringUtils.isEmpty(person.getString("homepage")) && !person.getString("homepage").equals("null"))
                    properties.put("homepage", new String[]{person.getString("homepage")});
                if (!StringUtils.isEmpty(person.getString("profile_path")) && !person.getString("profile_path").equals("null"))
                    properties.put("profile", new String[]{baseUrl + configuration.getJSONObject("images").getJSONArray("profile_sizes").get(2) + person.getString("profile_path")});
                if (!StringUtils.isEmpty(person.getString("birthday")) && !person.getString("birthday").equals("null"))
                    properties.put("birthday", new String[]{person.getString("birthday") + "T00:00:00.000+00:00"});
                if (!StringUtils.isEmpty(person.getString("deathday")) && !person.getString("deathday").equals("null"))
                    properties.put("deathday", new String[]{person.getString("deathday") + "T00:00:00.000+00:00"});

                ExternalData data = new ExternalData(identifier, "/persons/" + personId, "jnt:moviePerson", properties);
                return data;
            }
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }

        throw new ItemNotFoundException(identifier);
    }

    private String getPathForMovie(JSONObject movie) throws JSONException {
        if (StringUtils.isEmpty(movie.getString("release_date"))) {
            return null;
        }
        return "/movies/" + StringUtils.substringBeforeLast(movie.getString("release_date"), "-").replace("-", "/") + "/" + movie.getString("id");
    }

    /**
     * As getItemByIdentifier, get an ExternalData by its path
     *
     * @param path
     * @return ExternalData
     * @throws javax.jcr.PathNotFoundException
     *
     */
    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        String[] splitPath = path.split("/");
        try {
            if (path.endsWith("j:acl")) {
                throw new PathNotFoundException(path);
            }
            if (splitPath.length <= 1) {
                return getItemByIdentifier("root");
            } else if (splitPath[1].equals("movies")) {
                switch (splitPath.length) {
                    case 2:
                        return getItemByIdentifier("movies-rootfolder");
                    case 3:
                        return getItemByIdentifier("movies-folder-" + splitPath[2]);
                    case 4:
                        return getItemByIdentifier("movies-folder-" + splitPath[2] + "/" + splitPath[3]);
                    case 5:
                        return getItemByIdentifier("movie-" + splitPath[4]);
                    case 6:
                        return getItemByIdentifier("moviecredits-" + splitPath[4] + "-" + splitPath[5]);
                }
            } else if (splitPath[1].equals("lists")) {
                switch (splitPath.length) {
                    case 2:
                        return getItemByIdentifier("lists-rootfolder");
                    case 3:
                        return getItemByIdentifier("lists-" + splitPath[2]);
                    case 4:
                        return getItemByIdentifier("movieref-" + splitPath[2] + "-" + splitPath[3]);
                }
            } else if (splitPath[1].equals("persons")) {
                switch (splitPath.length) {
                    case 2:
                        return getItemByIdentifier("persons-rootfolder");
                    case 3:
                        return getItemByIdentifier("person-" + splitPath[2]);
                }
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
        throw new PathNotFoundException();
    }

    /**
     * Returns a set of supported node types.
     *
     * @return a set of supported node types
     */
    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet("jnt:contentFolder", "jnt:movie", "jnt:moviesList", "jnt:cast", "jnt:crew");
    }

    /**
     * Indicates if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system paths.
     *
     * @return <code>true</code> if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system
     *         paths; <code>false</code> otherwise.
     */
    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    /**
     * Indicates if the data source supports UUIDs.
     *
     * @return <code>true</code> if the data source supports UUIDs
     */
    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    /**
     * Returns <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>.
     *
     * @param path item path
     * @return <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>
     */
    @Override
    public boolean itemExists(String path) {
        return false;
    }

    private JSONObject queryTMDB(String path, String... params) throws RepositoryException {
        try {
            HttpURL url = new HttpURL(API_URL, 80, path);

            Map<String, String> m = new LinkedHashMap<String, String>();
            for (int i = 0; i < params.length; i += 2) {
                m.put(params[i], params[i + 1]);
            }
            m.put(API_KEY, apiKeyValue);

            url.setQuery(m.keySet().toArray(new String[m.size()]), m.values().toArray(new String[m.size()]));
            long l = System.currentTimeMillis();
            System.out.println("Start request : " + url);
            GetMethod httpMethod = new GetMethod(url.toString());
            try {
                httpClient.getParams().setSoTimeout(10000);
                httpClient.executeMethod(httpMethod);
                return new JSONObject(httpMethod.getResponseBodyAsString());
            } finally {
                httpMethod.releaseConnection();
                System.out.println("Request " + url + " done in "+(System.currentTimeMillis()-l) + "ms");
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }


    @Override
    public String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return getI18nPropertyValues(path, "en", propertyName);
    }

    @Override
    public String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException {
        String result;
        try {
            JSONObject movie;
            if (path.startsWith("/movies")) {
                String movieId = StringUtils.substringAfterLast(path, "/");
                if (cache.get("fullmovie-" + lang + "-" + movieId) != null) {
                    movie = new JSONObject((String) cache.get("fullmovie-" + lang + "-" + movieId).getObjectValue());
                } else {
                    movie = queryTMDB(API_MOVIE + movieId, "language", lang);
                    cache.put(new Element("fullmovie-" + lang + "-" + movieId, movie.toString()));
                }
                if (propertyName.equals("jcr:title") && movie.getString("title") != null) {
                    return new String[]{movie.getString("title")};
                } else if (propertyName.equals("poster_path") && movie.getString("poster_path") != null) {
                    JSONObject configuration = getConfiguration();
                    String baseUrl = configuration.getJSONObject("images").getString("base_url");
                    return new String[]{baseUrl + configuration.getJSONObject("images").getJSONArray("poster_sizes").get(1) + movie.getString(propertyName)};
                } else if (movie.getString(propertyName) != null) {
                    return new String[]{movie.getString(propertyName)};
                }
                return new String[]{""};
            }
        } catch (JSONException e) {
            throw new PathNotFoundException(e);
        } catch (RepositoryException e) {
            throw new PathNotFoundException(e);
        }
        return new String[0];
    }

    @Override
    public Binary[] getBinaryPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return new Binary[0];
    }

    public JSONObject getConfiguration() throws JSONException, RepositoryException {
        JSONObject configuration;
        if (cache.get("configuration") != null) {
            configuration = new JSONObject((String) cache.get("configuration").getObjectValue());
        } else {
            configuration = queryTMDB(API_CONFIGURATION);
            cache.put(new Element("configuration", configuration.toString()));
        }

        return configuration;
    }

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        String nodeType = QueryHelper.getNodeType(query.getSource());

        try {
            if (NodeTypeRegistry.getInstance().getNodeType("jnt:movie").isNodeType(nodeType)) {
                JSONArray tmdbResult = null;
                String year;
                String month;

                Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());

                if (m.containsKey("jcr.title")) { // TODO: updated : m.containsKey("jcr:title")
                    tmdbResult = queryTMDB(API_SEARCH_MOVIE, "query", m.get("jcr.title").getString()).getJSONArray("results"); // TODO: updated : m.get("jcr:title").getString()
                }

                // TODO: added
                if (m.containsKey("imdb_id")) {
                    tmdbResult = queryTMDB(API_FIND + m.get("imdb_id").getString(), "external_source", "imdb_id").getJSONArray("movie_results");
                }

                if (tmdbResult != null) {
                    for (int i = 0; i < tmdbResult.length(); i++) {
                        final String path = getPathForMovie(tmdbResult.getJSONObject(i));
                        if (path != null) {
                            results.add(path);
                        }
                    }
                }
            }
            if (NodeTypeRegistry.getInstance().getNodeType("jnt:moviesList").isNodeType(nodeType)) {
                Map<String, Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
                if (m.isEmpty()) {
                    JSONObject o = queryTMDB("/3/account/" + getAccountId() + "/lists", "session_id", getSessionId());
                    JSONArray result = o.getJSONArray("results");
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject list = result.getJSONObject(i);
                        results.add("/lists/" + list.getString("id"));
                    }
                }
            }

            if (NodeTypeRegistry.getInstance().getNodeType("jnt:cast").isNodeType(nodeType)) {
                Map<String,Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
                if (m.containsKey("id")) {
                    final String id = m.get("id").getString();
                    JSONObject search;
                    if (cache.get("movie_credits_query_"+id) != null) {
                        search = new JSONObject((String) cache.get("movie_credits_query_"+id).getObjectValue());
                    } else {
                        search = queryTMDB("/3/person/" + id + "/movie_credits");
                        cache.put(new Element("movie_credits_query_"+id, search.toString()));
                    }

                    JSONArray result = search.getJSONArray("cast");
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject r = result.getJSONObject(i);
                        ExternalData d = getItemByIdentifier("movie-" + r.getString("id"));
                        if (d != null && d.getPath() != null) {
                            for (String s : getChildren(d.getPath())) {
                                if (s.endsWith(id)) {
                                    results.add(d.getPath() + "/" + s);
                                    if (results.size() == 20) {
                                        return results;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (NodeTypeRegistry.getInstance().getNodeType("jnt:crew").isNodeType(nodeType)) {
                Map<String,Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
                if (m.containsKey("id")) {
                    final String id = m.get("id").getString();
                    JSONObject search;
                    if (cache.get("movie_credits_query_"+id) != null) {
                        search = new JSONObject((String) cache.get("movie_credits_query_"+id).getObjectValue());
                    } else {
                        search = queryTMDB("/3/person/" + id + "/movie_credits");
                        cache.put(new Element("movie_credits_query_"+id, search.toString()));
                    }

                    JSONArray result = search.getJSONArray("crew");
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject r = result.getJSONObject(i);
                        ExternalData d = getItemByIdentifier("movie-" + r.getString("id"));
                        if (d != null && d.getPath() != null) {
                            for (String s : getChildren(d.getPath())) {
                                if (s.endsWith("_"+r.getString("job") + "_" +id)) {
                                    results.add(d.getPath() + "/" + s);
                                    if (results.size() == 20) {
                                        return results;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedRepositoryOperationException e) {
            //
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
        return results;
    }

    private String getAccountId() throws RepositoryException, JSONException {
        if (accountId == null) {
            accountId = queryTMDB("/3/account", "session_id", getSessionId()).getString("id");
        }
        return accountId;
    }


    private String getSessionId() throws RepositoryException {
        if (token != null && sessionId == null) {
            JSONObject session = queryTMDB("/3/authentication/session/new", "request_token", token);
            token = null;
            try {
                sessionId = session.getString("session_id");
            } catch (JSONException e) {
                throw new RepositoryException("No open session");
            }
        }
        if (sessionId == null) {
            throw new RepositoryException("No open session");
        }
        return sessionId;
    }

    public String createToken() throws RepositoryException, JSONException {
        token = queryTMDB("/3/authentication/token/new").getString("request_token");
        sessionId = null;
        return token;
    }

}