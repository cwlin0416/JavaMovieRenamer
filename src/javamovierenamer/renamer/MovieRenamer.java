/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javamovierenamer.renamer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import net.sf.json.JSONObject;

/**
 *
 * @author cwlin
 */
public class MovieRenamer {

	private MovieRenameListener renameListener = null;
	private int totalTasks = 0;
	private int currentTasks = 0;

	public MovieRenamer() {
		this.setListener(new CliMovieRenameListener());
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File("/media/cwlin/6AFB73BA67D29A75/mislabftp/"));
		int result = chooser.showOpenDialog(null);

		MovieRenamer renamer = new MovieRenamer();
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			final File folder = new File(path);
			renamer.startRename(folder);
		}
	}

	public void setListener(MovieRenameListener listener) {
		this.renameListener = listener;
	}

	public void startRename(final File folder) {
		if (folder == null) {
			return;
		}
		if (folder.listFiles() == null) {
			return;
		}
		this.totalTasks += folder.listFiles().length;
		for (final File fileEntry : folder.listFiles()) {
			this.renameListener.changeProgress(this.totalTasks, ++this.currentTasks);
			if (fileEntry.isDirectory()) {
				this.startRename(fileEntry);
			} else {
				String extension = getFileExtension(fileEntry);
				if (extension == null) {
					continue;
				}
				if (!extension.equals("avi") && !extension.equals("mkv") && !extension.equals("mp4")) {
					continue;
				}
				try {
					this.renameListener.changeStatus("Quering... " + fileEntry.getName());
					MovieData data = getMovieData(fileEntry);
					String newName = data.getFileDirectoryName();
					File parent = fileEntry.getParentFile();
					String oriName = parent.getName();
					this.renameListener.changeStatus("Got chinese movie name..." + data.getChineseName());

					if (newName != null && !newName.equals(oriName)) {
						if (this.renameListener.renameMovie(oriName, newName, data)) {
							String path = parent.getParent() + "/" + newName;
							System.out.println(path);
							File newDir = new File(path);

							if (newDir.exists()) {
								this.renameListener.changeStatus("New dirname " + newName + " exists.");
							}
							if (parent.renameTo(newDir)) {
								this.renameListener.changeStatus("Rename successfully.");
							} else {
								this.renameListener.changeStatus("Rename failed.");
							}
						}
					}

				} catch (Exception e) {
					System.out.println("listFilesForFolder:" + e);
				}
			}

		}
	}

	public static String getFileExtension(File f) {
		String fileName = f.getName();
		String extension = null;
		int i = fileName.lastIndexOf(".");
		if (i > 0) {
			extension = fileName.substring(i + 1);
		}
		return extension;
	}

	public static File getMovieDirectory(File movieFile) {
		File parentDirectory = movieFile.getParentFile();
		System.out.println(parentDirectory.getAbsolutePath());
		return parentDirectory;
	}

	public static MovieData parseMovieFileName(File fileEntry) throws Exception {
		String fileName = fileEntry.getName();
		System.out.println(fileName);
		MovieData data = null;
		if (data == null) {
			// Rule1: Braveheart.1995.1080p.BrRip.x264.YIFY+HI
			//        ^^^^^^^^^^ ^^^^ ^^^^^ ^^^^^^^^^^^^^^^^^^
			//        Name       Year Res   Publisher
			Pattern p = Pattern.compile("(.*)[ .]\\(?(\\d{4})\\)?.*[ .](\\d{3,4}p)[ .]?(.*)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(fileName);
			if (m.matches()) {
				data = new MovieData();
				data.file = fileEntry;
				data.fileMovieName = m.group(1);
				data.fileMovieYear = m.group(2);
				data.fileResolution = m.group(3);
				data.filePublisher = m.group(4);
				System.out.println("Match Rule 1: " + data.getRawDataString());
			}
		}
		if (data == null) {
			// Rule2: Der Untergang (Downfall) (2004) [1080p] x264 - Jalucian
			//        ^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^   ^^^^^  ^^^^^^^^^^^^^^^^^^
			//        Name                      Year   Res    Publisher
			Pattern p = Pattern.compile("(.*)[ .]\\(?(\\d{4})\\)?.*[ .]\\[(\\d{3,4}p)\\][ .]?(.*)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(fileName);

			if (m.matches()) {
				data = new MovieData();
				data.file = fileEntry;
				data.fileMovieName = m.group(1);
				data.fileMovieYear = m.group(2);
				data.fileResolution = m.group(3);
				data.filePublisher = m.group(4);
				System.out.println("Match Rule 2: " + data.getRawDataString());
			}
		}
		if (data == null) {
			// Rule3: Inglourious.Basterds.1080p.BluRay.x264.anoXmous_
			//        ^^^^^^^^^^^^^^^^^^^^ ^^^^^  ^^^^^^^^^^^^^^^^^^
			//        Name                 Res    Publisher
			Pattern p = Pattern.compile("(.*)[ .](\\d{3,4}p)[ .]?(.*)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(fileName);

			if (m.matches()) {
				data = new MovieData();
				data.file = fileEntry;
				data.fileMovieName = m.group(1);
				data.fileResolution = m.group(2);
				data.filePublisher = m.group(3);
				System.out.println("Match Rule 3: " + data.getRawDataString());
			}
		}
		if (data == null) {
			// Rule4: Mononoke.hime.[Princess.Mononoke].1997.HDTVRip.H264.AAC.Gopo
			//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^ ^^^^^^^^^^^^^^^^^^^^^
			//        Name                              Year Publisher
			Pattern p = Pattern.compile("(.*)[ .]\\(?(\\d{4})\\)?[ .](.*)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(fileName);

			if (m.matches()) {
				data = new MovieData();
				data.file = fileEntry;
				data.fileMovieName = m.group(1);
				data.fileMovieYear = m.group(2);
				data.filePublisher = m.group(3);
				System.out.println("Match Rule 4: " + data.getRawDataString());
			}
		}
		return data;
	}

	public static MovieData getMovieData(File movieFile) {
		try {
			MovieData data = MovieRenamer.parseMovieFileName(movieFile);
			OmdbApi omdb = new OmdbApi();
			AtMovieApi atMovie = new AtMovieApi();
			data = atMovie.queryMovieData(data);
			data = omdb.queryMovieData(data);
			return data;
		} catch (Exception ex) {
			System.out.println("getMovieData: " + ex);
		}
		return null;
	}

	public static String getMovieName(File movieFile) {
		try {
			MovieData data = getMovieData(movieFile);
			return data.getFileDirectoryName();
		} catch (Exception ex) {
			System.out.println("getMovieName: " + ex);
		}
		return null;
	}
}

class AtMovieApi {

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
			Pattern p = Pattern.compile(".*<span class=\"at21b\">(.*)</span><br>.*<span class=\"at12b_gray\">([A-Za-z0-9:.,&\\- '\\[\\]]+)</span>.*");
			Matcher m = p.matcher(result);
			if (m.matches()) {
				data.atMovieChineseName = m.group(1).trim();
				data.atMovieEnglishName = m.group(2).trim();
				{
					// Contagion [2001], remove year
					Pattern p2 = Pattern.compile("(.*) \\[\\d+\\]");
					Matcher m2 = p2.matcher(data.atMovieEnglishName);
					if (m2.matches()) {
						data.atMovieEnglishName = m2.group(1).trim();
					}
				}
				System.out.println("Name: " + data.atMovieChineseName + "(" + data.atMovieEnglishName + ")");
			}
			Pattern dateP = Pattern.compile(".*片長：(\\d+)分&nbsp; 上映日期：([0-9/]+)&nbsp; .*<BR>.*");
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
			data.atMovieReleaseDate = date;
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

class OmdbApi {

	private String omdbapiUrl = "http://www.omdbapi.com/";
	private final String USER_AGENT = "Mozilla/5.0";

	public static String formatDate(String dateString) {
		if (dateString == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
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
			String jsonResult = this.sendGet(url);

			JSONObject jsonObject = JSONObject.fromObject(jsonResult);
			data.imdbEnglishName = (String) jsonObject.get("Title");
			data.imdbRating = (String) jsonObject.get("imdbRating");
			data.imdbVotes = (String) jsonObject.get("imdbVotes");
			data.imdbReleaseDate = formatDate((String) jsonObject.get("Released"));
			data.imdbYear = (String) jsonObject.get("Year");
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
