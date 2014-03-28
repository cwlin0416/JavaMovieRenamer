/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javamovierenamer;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author cwlin
 */
class XmlFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		return f.getName().toLowerCase().endsWith(".xml") || f.isDirectory();
	}

	@Override
	public String getDescription() {
		return "BDN XML files (*.xml)";
	}
}

class SrtFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		return f.getName().toLowerCase().endsWith(".srt") || f.isDirectory();
	}

	@Override
	public String getDescription() {
		return "SubRip file (*.srt)";
	}
}

public class BDN2Srt {

	public static void main(String[] args) {
		File in = null;
		File out = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new XmlFilter());
		chooser.setCurrentDirectory(new File("/home/cwlin/Dropbox/subtitles/The Wrestler"));
		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			in = new File(path);
		}
		JFileChooser chooser2 = new JFileChooser();
		chooser2.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser2.setFileFilter(new SrtFilter());
		chooser2.setCurrentDirectory(new File(chooser.getSelectedFile().getAbsolutePath()));
		int result2 = chooser2.showSaveDialog(null);
		if (result2 == JFileChooser.APPROVE_OPTION) {
			String path = chooser2.getSelectedFile().getAbsolutePath();
			out = new File(path);
		}

		BDN2Srt parser = new BDN2Srt();
		parser.readBdn(in, out);
	}

	public static String BDNTimeToSrtTime(String time, Float frameRate) {
		String timeHead = time.substring(0, time.lastIndexOf(":"));
		String timeTail = time.substring(time.lastIndexOf(":") + 1);
		int ms = Integer.parseInt(timeTail);
		int convertedMs = (int) ((1000 / frameRate) * ms);
		if (convertedMs >= 1000) {
			convertedMs = 999;
		}
		return String.format("%s,%03d", timeHead, convertedMs);

	}

	public static void pngTobmp(File input, File output) throws IOException {
		//Read the file to a BufferedImage
		BufferedImage image = ImageIO.read(input);

		//Write the image to the destination as a BMP
		ImageIO.write(image, "bmp", output);
		return;
	}

	public void readBdn(File inFile, File outFile) {
		boolean pngToBmp = true;
		long lasting = System.currentTimeMillis();

		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF8"));

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(inFile);

			NodeList formatNl = doc.getElementsByTagName("Format");
			Element formatElement = (Element) formatNl.item(0);
			Float frameRate = Float.parseFloat(formatElement.getAttribute("FrameRate"));
			NodeList eventNl = doc.getElementsByTagName("Event");

			System.out.println(eventNl.getLength());
			for (int i = 0; i < eventNl.getLength(); i++) {
				Node eventNode = eventNl.item(i);
				Element eventElement = (Element) eventNode;
				NodeList graphNodes = eventNode.getChildNodes();
				String startTime = eventElement.getAttribute("InTC");
				String endTime = eventElement.getAttribute("OutTC");
				startTime = BDN2Srt.BDNTimeToSrtTime(startTime, frameRate);
				endTime = BDN2Srt.BDNTimeToSrtTime(endTime, frameRate);

				System.out.println(i + 1);
				System.out.println(startTime);
				System.out.println(endTime);

				out.append(String.format("%d", (i + 1))).append("\r\n");
				out.append(startTime);
				out.append(" --> ");
				out.append(endTime).append("\r\n");

				for (int j = 0; j < graphNodes.getLength(); j++) {
					String nodeName = graphNodes.item(j).getNodeName();
					if (!nodeName.equals("Graphic")) {
						continue;
					}
					Element graphNode = (Element) graphNodes.item(j);
					String text = graphNode.getTextContent();
					if( pngToBmp ) {
						text = text.replace("png", "bmp");
					}
					System.out.println(text);
					out.append(text).append("\r\n");
				}
				out.append("\r\n");
				out.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
