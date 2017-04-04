package renamer.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import renamer.MovieData;

/**
 *
 * @author Cwlin
 */
public class TmdbApi implements RenamerApiInterface {

    private String apiUrl = "https://api.themoviedb.org/3/search/movie";
    private String apiKey = ""; // Please signup at https://www.themoviedb.org/ at request your own API.
    private String language = "zh-TW";
    private final String USER_AGENT = "Mozilla/5.0";

    public MovieData queryMovieData(MovieData data) {
        try {
            String url = this.apiUrl + "?api_key=" + this.apiKey + "&language=" + this.language + "&query=" + URLEncoder.encode(data.getFileMovieName(), "UTF-8");
            String jsonResult = this.sendGet(url);

            JSONObject jsonObject = JSONObject.fromObject(jsonResult);
            JSONArray results = jsonObject.getJSONArray("results");
            // Compare en titles
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                String title = item.getString("title");
                String overview = item.getString("overview");
                String releaseDate = item.getString("release_date");
                String originalTitle = item.getString("original_title");

                if (data.chineseName == null) {
                    data.chineseName = title;
                    data.releaseDate = releaseDate;
                    data.overview = overview;

                    if (originalTitle != null && !originalTitle.toLowerCase().trim().equals(data.getFileMovieName().toLowerCase().trim())) {
                        System.out.println("==============================================");
                        System.out.println("!!Be carefully english name not excetly same (" + originalTitle + " vs " + data.getFileMovieName() + ")!!");
                        System.out.println("==============================================");
                    }
                    break;
                }
            }

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
