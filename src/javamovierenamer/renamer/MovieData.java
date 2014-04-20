/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javamovierenamer.renamer;

import java.io.File;
import static javamovierenamer.renamer.MovieRenamer.getMovieDirectory;
import static javamovierenamer.renamer.MovieRenamer.getMovieName;

/**
 *
 * @author cwlin
 */
public class MovieData {
	// Original movie file info

	public File file;
	String fileMovieName;
	String fileMovieYear;
	String fileResolution;
	String filePublisher;
	// Queryed movie info
	String atMovieChineseName = null;
	String atMovieEnglishName = null;
	String atMovieReleaseDate = null;
	String atMovieType = null;
	String atMovieLink = null;
	String atMovieFilmId = null;
	int atMovieLength = 0;
	String imdbEnglishName = null;
	String imdbReleaseDate = null;
	public String imdbRating = null;
	public String imdbVotes = null;
	String imdbYear = null;

	@Override
	public String toString() {
		try {
			return this.getFileDirectoryName();
		} catch (Exception e) {
			return null;
		}
	}
	public String getRawDataString() {
		return this.fileMovieName + ", " + this.fileMovieYear + ", " + this.fileResolution + ", " + this.filePublisher;
	}

	public String getFileDirectoryName() throws Exception {
		String name = this.getChineseName();
		String releaseDate = this.getReleaseDate();

		if (name == null) {
			name = this.getEnglishName();
		}

		if (this.imdbRating != null) {
			this.imdbRating = this.imdbRating.replace("/", "-");
		} else if (this.imdbRating == "N/A") {
			this.imdbRating = null;
		}
		String fullname = releaseDate + "-" + name + (this.imdbRating == null ? "" : "(" + this.imdbRating + ")");
		if (this.fileResolution != null && !this.fileResolution.isEmpty()) {
			if (this.fileResolution.equals("1080")) {
				this.fileResolution += "p";
			}
			this.fileResolution = this.fileResolution.toLowerCase();
			if (this.fileResolution.equals("1080p")) {
				fullname += " [" + this.fileResolution + "]";
			}
		}

		if (releaseDate == null || name == null) {
			throw new Exception("檔名資訊不齊全: " + fullname);
		}
		return fullname;
	}

	public String getFileMovieName() {
		String title = this.fileMovieName;
		title = title.replace(".", " ");
		return title;
	}

	public String getChineseName() {
		if (this.atMovieChineseName != null) {
			return this.atMovieChineseName.replace(":", "：");
		}
		return this.atMovieChineseName;
	}

	public String getEnglishName() {
		if (this.atMovieEnglishName == null) {
			return this.imdbEnglishName;
		}
		return this.atMovieEnglishName;
	}

	public String getReleaseDate() {
		if (this.atMovieReleaseDate == null) {
			return this.imdbReleaseDate;
		}
		return this.atMovieReleaseDate;
	}

	public void printFileInfo() {
		boolean isDebug = false;
		File parent = this.file.getParentFile();
		System.out.println("Fullname: " + this.file.getName());
		if (isDebug) {
			System.out.println("Name:" + this.getFileMovieName());
			System.out.println("Year: " + this.fileMovieYear);
			System.out.println("Resoultion: " + this.fileResolution);
			System.out.println("Publisher: " + this.filePublisher);
		}
		System.out.println("Chinese name: " + this.getChineseName());
		System.out.println("English name: " + this.getEnglishName());
		System.out.println("Release date: " + this.getReleaseDate());
		if (isDebug) {
			System.out.println("atMovie Chinese name: " + this.atMovieChineseName);
			System.out.println("atMovie English name: " + this.atMovieEnglishName);
			System.out.println("atMovie Release date: " + this.atMovieReleaseDate);
			System.out.println("atMovie Type: " + this.atMovieType);
			System.out.println("atMovie Link: " + this.atMovieLink);
			System.out.println("atMovie Film id: " + this.atMovieFilmId);

			System.out.println("IMDB English name: " + this.imdbEnglishName);
			System.out.println("IMDB Release date: " + this.imdbReleaseDate);
			System.out.println("IMDB Year: " + this.imdbYear);
		}
		System.out.println("IMDB Rating: " + this.imdbRating);
		System.out.println("IMDB Votes: " + this.imdbVotes);
		System.out.println("Path: " + parent.getAbsolutePath());
		System.out.println("Current Directory Name: " + parent.getName());
		System.out.println("Changed Name: " + this.toString());
	}
}
