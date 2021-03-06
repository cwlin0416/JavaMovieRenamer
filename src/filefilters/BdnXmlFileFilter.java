/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package filefilters;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Cwlin
 */
public class BdnXmlFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		return f.getName().toLowerCase().endsWith(".xml") || f.isDirectory();
	}

	@Override
	public String getDescription() {
		return "BDN XML files (*.xml)";
	}
}