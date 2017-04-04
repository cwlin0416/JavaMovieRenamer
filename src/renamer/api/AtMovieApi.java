package renamer.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import renamer.MovieData;

/**
 *
 * @author Cwlin
 */
public class AtMovieApi implements RenamerApiInterface {

    private final String USER_AGENT = "Mozilla/5.0";

    class AtMovieData {

        String atMovieLinkName = null;
        String atMovieType = null;
        String atMovieLink = null;
        String atMovieFilmId = null;
    }

    public MovieData queryMovieData(MovieData data) {

        String title = data.getFileMovieName();
        List<AtMovieData> list = new ArrayList<>();
        try {
            String url = "http://search.atmovies.com.tw/search/search.cfm";

            // Encode the query 
            String encodedQuery = URLEncoder.encode(title, "UTF-8");
            // This is the data that is going to be send to itcuties.com via POST request
            // 'e' parameter contains data to echo
            String postData = "type=F&search_term=" + encodedQuery + "&search=" + URLEncoder.encode("提交", "UTF-8") + "&action=home&fr=search";
            String htmlResult = this.sendPost(url, postData);

            String after = htmlResult;

            after = after.replaceAll("(.*)<OL>(.*)</ol>(.*)", "$2");
            after = after.replaceAll("</div>", "</div>\r\n");
            String[] lines = after.split("\r\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                Pattern itemP = Pattern.compile(".*<LI><font.*>(.*)</font><a href=\"(.*)\".*>(.*)</a>.*");
                Matcher itemM = itemP.matcher(line);
                if (!itemM.matches()) {
                    continue;
                }
                String link = itemM.group(2).trim();
                Pattern linkP = Pattern.compile("redirect.cfm\\?p=F&d=([A-Za-z0-9]+)");
                Matcher linkM = linkP.matcher(link);
                if (!linkM.matches()) {
                    continue;
                }

                AtMovieData atMovieData = new AtMovieData();
                atMovieData.atMovieType = itemM.group(1).trim();
                atMovieData.atMovieLink = link;
                atMovieData.atMovieLinkName = itemM.group(3).trim();
                atMovieData.atMovieFilmId = linkM.group(1);
                System.out.println("AtMovie Search List: " + atMovieData.atMovieLinkName + ", FilmId: " + atMovieData.atMovieFilmId);
                list.add(atMovieData);
            }

            // 有結果
            if (list.size() > 0) {
                MovieData tempData;
                boolean isFound = false;
                for (int i = 0; i < list.size(); i++) {
                    AtMovieData atMovieData = list.get(i);
                    data.atMovieEnglishName = title;
                    data.atMovieFilmId = atMovieData.atMovieFilmId;
                    data.atMovieLink = atMovieData.atMovieLink;
                    data.atMovieType = atMovieData.atMovieType;
                    tempData = this.queryMovieDetailData(data);
                    if (tempData.atMovieEnglishName != null && tempData.atMovieEnglishName.toLowerCase().trim().equals(title.toLowerCase().trim())) {
                        isFound = true;
                        break;
                    }
                    tempData.atMovieEnglishName = tempData.atMovieEnglishName.replaceAll("-", " ");
                    title = title.replaceAll("-", " ");
                    if (tempData.atMovieEnglishName != null && tempData.atMovieEnglishName.toLowerCase().trim().equals(title.toLowerCase().trim())) {
                        isFound = true;
                        break;
                    }
                }
                if (isFound == false) {
                    System.out.println("==============================================");
                    System.out.println("!!Be carefully english name not excetly same!!");
                    System.out.println("==============================================");
                }
                return data;
            }
        } catch (Exception e) {
            //Logger.getLogger(AtMovieApi.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("getMovieDataByTitle: " + e);
        }
        return data;
    }

    public MovieData queryMovieDetailData(MovieData data) {
        String filmId = data.atMovieFilmId;
        String url = "http://app.atmovies.com.tw/movie/movie.cfm?action=filmdata&film_id=" + filmId;
        String date = null;
        String length = null;
        try {
            String result = this.sendGet(url);
            //Pattern p = Pattern.compile(".*<span class=\"at21b\">(.*)</span><br>.*<span class=\"at12b_gray\">([A-Za-z0-9:.,&\\- '\\[\\]]+)</span>.*");
            Pattern p = Pattern.compile(".*<div class=\"filmTitle\"><!-- filmTitle -->(.*)</div><!-- filmTitle end -->.*");
            Matcher m = p.matcher(result);
            if (m.matches()) {
                String fullname = m.group(1);
                fullname = fullname.replaceAll("<(.|\n)*?>", "");
                fullname = fullname.trim();
                System.out.println(fullname);
                Pattern p2 = Pattern.compile("(([0-9A-Za-z\\[\\]]|[^\\x00-\\x40\\x5B-\\x60\\x7B-\\x7F])+) ([A-Za-z0-9:.,&\\- '\\[\\]]+)");
                Matcher m2 = p2.matcher(fullname);
                if (m2.matches()) {
                    data.chineseName = m2.group(1).trim();
                    data.atMovieEnglishName = m2.group(3).trim();
                    Pattern p3 = Pattern.compile("(.*) \\[\\d+\\]");
                    Matcher m3 = p3.matcher(data.atMovieEnglishName);
                    if (m3.matches()) {
                        data.atMovieEnglishName = m3.group(1).trim();
                    }
                }
                System.out.println("Name: " + data.chineseName + "(" + data.atMovieEnglishName + ")");
            }
            Pattern dateP = Pattern.compile(".*片長：(\\d+)分</li>.*上映日期：([0-9/]+)</li>.*<BR>.*");
            Matcher dateM = dateP.matcher(result);
            if (dateM.matches()) {
                length = dateM.group(1);
                date = dateM.group(2).replaceAll("/", "-");
            }
            try {
                data.atMovieLength = Integer.parseInt(length);
            } catch (Exception e) {
                data.atMovieLength = 0;
            }
            data.releaseDate = date;

            Pattern imdbIdP = Pattern.compile(".*<a  href=\"http://us.imdb.com/Title\\?(.*)\" target=_blank>IMDb</a>.*");
            Matcher imdbIdM = imdbIdP.matcher(result);
            if (imdbIdM.matches()) {
                String imdbId = "tt" + imdbIdM.group(1);
                data.imdbId = imdbId;
            }
        } catch (Exception e) {
            System.out.println("getMovieDetailData: " + e);
        }
        return data;
    }

    // HTTP GET request
    private String sendPost(String url, String postData) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // 模擬由 search.atmovies.com.tw 發出 request
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        con.setRequestProperty("Host", "search.atmovies.com.tw");
        con.setRequestProperty("Origin", "http://search.atmovies.com.tw");
        con.setRequestProperty("Referer", "http://search.atmovies.com.tw/search/search.cfm");

        // optional default is GET
        con.setRequestMethod("POST");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Content-Length", String.valueOf(postData.length()));

        // Write data
        OutputStream os = con.getOutputStream();
        os.write(postData.getBytes());

        int responseCode = con.getResponseCode();
//		System.out.println("\nSending 'POST' request to URL : " + url);
//		System.out.println("Content : " + postData);
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
