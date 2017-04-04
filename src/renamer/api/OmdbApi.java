package renamer.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.sf.json.JSONObject;
import renamer.MovieData;

/**
 *
 * @author Cwlin
 */
public class OmdbApi implements RenamerApiInterface {

    private String omdbapiUrl = "http://www.omdbapi.com/";
    private final String USER_AGENT = "Mozilla/5.0";

    public static String formatDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        try {
            Date date = sdf.parse(dateString);
            return String.format("%4d-%02d-%02d", date.getYear() + 1900, date.getMonth(), date.getDate());
        } catch (Exception e) {
            System.out.println("OmdbApi: formatDate(): " + e);
        }
        return null;
    }

    public MovieData queryMovieData(MovieData data) {
        try {
            String url = this.omdbapiUrl + "?t=" + URLEncoder.encode(data.getFileMovieName(), "UTF-8");
            if (data.imdbId != null) {
                url = this.omdbapiUrl + "?i=" + data.imdbId;
                System.out.println("Get omdb data by imdb id: " + data.imdbId);
            }
            if (data.fileMovieYear != null) {
                url += "&y=" + data.fileMovieYear;
            }
            String jsonResult = this.sendGet(url);

            JSONObject jsonObject = JSONObject.fromObject(jsonResult);
            data.imdbEnglishName = (String) jsonObject.get("Title");
            data.imdbRating = (String) jsonObject.get("imdbRating");
            data.imdbVotes = (String) jsonObject.get("imdbVotes");
            data.imdbReleaseDate = formatDate((String) jsonObject.get("Released"));
            data.imdbYear = (String) jsonObject.get("Year");

            System.out.println("Get english Name: " + data.imdbEnglishName);
            System.out.println("Get imdb rating: " + data.imdbRating);
            System.out.println("Get imdb vote: " + data.imdbVotes);
            System.out.println("Get imdb release date: " + data.imdbReleaseDate);
            System.out.println("Get imdb year: " + data.imdbYear);
            return data;
        } catch (Exception e) {
            System.out.println("getMovieDataByTitle: " + e);
        }
        return data;
    }

    // HTTP GET request
    private String sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
//		System.out.println("\nSending 'GET' request to URL : " + url);
//		System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();

    }
}
