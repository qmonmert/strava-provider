### External data provider

1. API strava

    * http://strava.github.io/api/
    * Example
        * https://www.strava.com/api/v3/athlete?access_token=xxx

2.  Apply

    * Code
        * Complete the spring file : strava-provider.xml with your strava access_token

    * Admin Jahia
        * Create a site named : strava-site
        * Deploy the module strava-provider on the site

    * Edit Mode Jahia
        * Create a new page in the site
        * Add a component named 'athlete' in this page
        * Look the result

3.  JCR

    * Go to http://localhost:8080/modules/tools/jcrQuery.jsp
    * Query :
        * JCR-SQL2
        * SELECT * FROM [jnt:stravaAccount]
