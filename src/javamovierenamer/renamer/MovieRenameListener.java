/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package javamovierenamer.renamer;

/**
 *
 * @author Cwlin
 */
public interface MovieRenameListener {
	public boolean renameMovie(String oriName, String newName, MovieData movieData);
	public void changeStatus(String status);
	public void changeProgress(int total, int current);
}
