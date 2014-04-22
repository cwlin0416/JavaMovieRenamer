/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renamer;

/**
 *
 * @author Cwlin
 */
public class CliMovieRenameListener implements MovieRenameListener {

	@Override
	public boolean renameMovie(String oriName, String newName, MovieData movieData) {
		try {
			int key;

			movieData.printFileInfo();
			// Prompt to change name
			System.out.print("\nDo you want to change name? (y/N)");
			do {
				key = System.in.read();
			} while (key == 10);

			if (key == 121) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("CliMovieRenameListener: renameMovie: " + e);
		}
		System.out.println("Skip...");
		return false;
	}

	@Override
	public void changeStatus(String status) {
		System.out.println(status);
	}

	@Override
	public void changeProgress(int total, int current) {
		System.out.println("Progress: " + current + "/" + total);
	}

}
