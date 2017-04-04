/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package renamer;

import renamer.api.AtMovieApi;
import renamer.api.OmdbApi;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import renamer.api.RenamerApiInterface;
import renamer.api.TmdbApi;

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
            Class[] classes = {AtMovieApi.class, TmdbApi.class, OmdbApi.class};
            for (int i = 0; i < classes.length; i++) {
                Class classe = classes[i];
                System.out.println("Querying " + classe.getTypeName() + "...");
                RenamerApiInterface api = (RenamerApiInterface) classe.newInstance();
                api.queryMovieData(data);
            }
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
