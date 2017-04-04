/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package renamer;

import java.io.File;
import static renamer.MovieRenamer.getMovieDirectory;
import static renamer.MovieRenamer.getMovieName;

/**
 *
 * @author cwlin
 */
public class MovieData {
    // Original movie file info

    public File file;
    public String fileMovieName;
    public String fileMovieYear;
    public String fileResolution;
    public String filePublisher;

    // Queried movie info
    public String chineseName = null;
    public String releaseDate = null;
    public String overview = null;
    public String imdbId = null; // Majorly from AtmovieApi
    public String imdbEnglishName = null; // Majorly from OmdbApi
    public String imdbReleaseDate = null; // Majorly from OmdbApi
    public String imdbRating = null; // Majorly from OmdbApi
    public String imdbVotes = null; // Majorly from OmdbApi
    public String imdbYear = null; // Majorly from OmdbApi

    // Atmovie data (atMovie temporary data, should be removed)
    public String atMovieEnglishName = null;
    public String atMovieType = null;
    public String atMovieLink = null;
    public String atMovieFilmId = null;
    public int atMovieLength = 0;

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
        if (this.chineseName != null) {
            return this.chineseName.replace(":", "：");
        }
        return this.chineseName;
    }

    public String getEnglishName() {
        if (this.atMovieEnglishName == null) {
            return this.imdbEnglishName;
        }
        return this.atMovieEnglishName;
    }

    public String getReleaseDate() {
        if (this.releaseDate == null) {
            return this.imdbReleaseDate;
        }
        return this.releaseDate;
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
            System.out.println("atMovie Chinese name: " + this.chineseName);
            System.out.println("atMovie English name: " + this.atMovieEnglishName);
            System.out.println("atMovie Release date: " + this.releaseDate);
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
