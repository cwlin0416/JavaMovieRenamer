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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
            String url = "http://search.atmovies.com.tw/search/";

            // Encode the query 
            String encodedQuery = URLEncoder.encode(title, "UTF-8");
            // This is the data that is going to be send to itcuties.com via POST request
            // 'e' parameter contains data to echo
            String postData = "type=all&search_term=" + encodedQuery + "&enc=UTF-8&fr=@-homepage";
            String htmlResult = this.sendPost(url, postData);

            Document doc = Jsoup.parse(htmlResult);
            Elements resultLinks = doc.select("#main > div > div > section > div > div > div > div > blockquote > header");
            //Elements resultLinks = doc.select("#main > div > div > section > div > div > div > div > blockquote > ol > li > a");
            for (int i = 0; i < resultLinks.size(); i++) {
                Element elem = resultLinks.get(i);
                String type = elem.select("font:nth-child(1)").first().text();
                String link = elem.select("a").first().attr("href");
                String linkName = elem.select("a").first().text();
                String year = elem.select("font:nth-child(3)").first().text();

                AtMovieData atMovieData = new AtMovieData();
                atMovieData.atMovieType = type;
                atMovieData.atMovieLink = link;
                atMovieData.atMovieLinkName = linkName;
                atMovieData.atMovieFilmId = link.replaceFirst("/F/", "");
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
                    System.out.println("!!Be carefully english name not excetly same (" + data.atMovieEnglishName + " vs " + title + ")!!");
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
        String url = "http://search.atmovies.com.tw/F/" + filmId;
        String chineseName = null, englishName = null, releaseDate = null, length = null, imdbId = null;
        try {
            String htmlResult = this.sendGet(url);
            Document doc = Jsoup.parse(htmlResult);

            String fullnameOri = doc.select("#main > div > div > div > cfprocessingdirective > div.filmTitle").first().text();
            fullnameOri = fullnameOri.replaceAll("<(.|\n)*?>", "");
            fullnameOri = fullnameOri.trim();
            Pattern p2 = Pattern.compile("(([0-9A-Za-z\\[\\]]|[^\\x00-\\x40\\x5B-\\x60\\x7B-\\x7F])+) ([A-Za-z0-9:.,&\\- '\\[\\]]+)");
            Matcher m2 = p2.matcher(fullnameOri);
            if (m2.matches()) {
                chineseName = m2.group(1).trim();
                englishName = m2.group(3).trim();;
                Pattern p3 = Pattern.compile("(.*) \\[\\d+\\]");
                Matcher m3 = p3.matcher(englishName);
                if (m3.matches()) {
                    englishName = m3.group(1).trim();
                }
            }

            String lengthOri = doc.select("#filmTagBlock > span:nth-child(3) > ul > li:nth-child(1)").first().text();
            Pattern lenP = Pattern.compile("片長：(\\d+)分");
            Matcher lenM = lenP.matcher(lengthOri);
            if (lenM.matches()) {
                length = lenM.group(1);
            }
            String dateOri = doc.select("#filmTagBlock > span:nth-child(3) > ul > li:nth-child(2)").first().text();
            Pattern dateP = Pattern.compile("上映日期：([0-9/]+)");
            Matcher dateM = dateP.matcher(dateOri);
            if (dateM.matches()) {
                releaseDate = dateM.group(1).replaceAll("/", "-");
            }
            String imdbLink = doc.select("#filmCastDataBlock > ul:nth-child(2) > li:nth-child(1) > a").first().attr("href");
            Pattern imdbIdP = Pattern.compile("http://us.imdb.com/Title\\?(.*)");
            Matcher imdbIdM = imdbIdP.matcher(imdbLink);
            if (imdbIdM.matches()) {
                imdbId = "tt" + imdbIdM.group(1);
            }

            try {
                data.atMovieLength = Integer.parseInt(length);
            } catch (Exception e) {
                data.atMovieLength = 0;
            }
            data.chineseName = chineseName;
            data.atMovieEnglishName = englishName;
            data.releaseDate = releaseDate;
            data.imdbId = imdbId;
            System.out.println("Get name: " + chineseName + "(" + englishName + ")");
            System.out.println("Get release date: " + releaseDate);
            System.out.println("Get length: " + length);
            System.out.println("Get imdb id: " + imdbId);
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
