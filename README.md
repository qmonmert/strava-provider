### External data provider

1.  API themoviedb

    * https://www.themoviedb.org/documentation/api
    * Example
        * Search : http://api.themoviedb.org/3/search/movie?query=spiderman&api_key=xxx
        * Find : http://api.themoviedb.org/3/find/tt0100669?api_key=xxx&external_source=imdb_id

2. API strava

    * http://strava.github.io/api/
    * Example
        * https://www.strava.com/api/v3/athlete?access_token=xxx

3.  Components

    * myMoviesList
    * stravaAccount

4.  Apply

    * Complete the spring file : tmdb-provider.xml

    * Deploy the module tmdb-provider

    * themoviedb
        * Create a new page in the ACMESPACE site
        * Add a 'myMoviesList' component in this page
        * Write spiderman in the title of the component
        * Look the result

    * strava
        * Create a new page in the ACMESPACE site
        * Add a 'stravaAccount' component in this page
        * Look the result


