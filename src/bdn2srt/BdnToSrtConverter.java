package bdn2srt;

import filefilters.SubRipFileFilter;
import filefilters.BdnXmlFileFilter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
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
public class BdnToSrtConverter {

	private BdnToSrtListener listener = null;

	public BdnToSrtConverter() {
		this.listener = new CliBdnToSrtListener();
	}
	public static void main(String[] args) {
		File in = null;
		File out = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new BdnXmlFileFilter());
		chooser.setCurrentDirectory(new File("/home/cwlin/Dropbox/subtitles/The Wrestler"));
		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			in = new File(path);
		}
		JFileChooser chooser2 = new JFileChooser();
		chooser2.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser2.setFileFilter(new SubRipFileFilter());
		chooser2.setCurrentDirectory(new File(chooser.getSelectedFile().getAbsolutePath()));
		int result2 = chooser2.showSaveDialog(null);
		if (result2 == JFileChooser.APPROVE_OPTION) {
			String path = chooser2.getSelectedFile().getAbsolutePath();
			out = new File(path);
		}

		BdnToSrtConverter parser = new BdnToSrtConverter();
		parser.readBdn(in, out);
	}

	public void setListener(BdnToSrtListener listener) {
		this.listener = listener;
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

	private static BufferedImage convertColorModel(BufferedImage src, int bufImgType) {
		BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), bufImgType);
		Graphics2D g2d = img.createGraphics();
		g2d.drawImage(src, 0, 0, null);
		g2d.dispose();
		return img;
	}

	private static BufferedImage convertTransparentToWhite(BufferedImage src) {
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				if (src.getRGB(x, y) == 0) {
					src.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		return convertColorModel(src, BufferedImage.TYPE_INT_RGB);
	}

	public static void pngToBmp(File input, File output) throws Exception {
		if (!input.exists()) {
			throw new Exception("Input file " + input.getPath() + " not exists.");
		}
		//Read the file to a BufferedImage
		BufferedImage image = ImageIO.read(input);
		// PNG 中有透明部份需要做轉換去除才有辦法存為 BMP
		BufferedImage convertedImage = convertTransparentToWhite(image);

		//Write the image to the destination as a BMP
		if (!ImageIO.write(convertedImage, "BMP", output)) {
			throw new Exception("Output file " + input.getPath() + " convert failed.");
		}
		return;
	}

	public void readBdn(File inFile, File outFile) {
		boolean pngToBmp = true;
		long lasting = System.currentTimeMillis();
		File inParentDirectory = inFile.getParentFile();
		File outParentDirectory = outFile.getParentFile();

		if (!inParentDirectory.isDirectory()) {
			System.out.println("inParentDirectory: " + inParentDirectory.toString() + " is not directory.");
			return;
		}
		if (!outParentDirectory.isDirectory()) {
			System.out.println("outParentDirectory: " + outParentDirectory.toString() + " is not directory.");
			return;
		}

		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF8"));

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(inFile);

			NodeList formatNl = doc.getElementsByTagName("Format");
			Element formatElement = (Element) formatNl.item(0);
			Float frameRate = Float.parseFloat(formatElement.getAttribute("FrameRate"));
			NodeList eventNl = doc.getElementsByTagName("Event");

			int total = eventNl.getLength();
			listener.changeStatus("Total length: " + total);
			for (int i = 0; i < total; i++) {
				Node eventNode = eventNl.item(i);
				Element eventElement = (Element) eventNode;
				NodeList graphNodes = eventNode.getChildNodes();
				String startTime = eventElement.getAttribute("InTC");
				String endTime = eventElement.getAttribute("OutTC");
				startTime = BdnToSrtConverter.BDNTimeToSrtTime(startTime, frameRate);
				endTime = BdnToSrtConverter.BDNTimeToSrtTime(endTime, frameRate);

				listener.changeStatus("Converting: " + (i+1) + "/" + total);

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
					if (pngToBmp) {
						String inPngFilePath = inParentDirectory.toString() + "/" + text;
						text = String.format("%04d.bmp", (i + 1));
						String outBmpFilePath = outParentDirectory.toString() + "/" + text;

						// convert file and save to destination
						BdnToSrtConverter.pngToBmp(new File(inPngFilePath), new File(outBmpFilePath));
					}
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
