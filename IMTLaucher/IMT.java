import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import ij.plugin.filter.*;
import java.io.*;
import java.nio.file.Paths;
import java.nio.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.nio.CharBuffer;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Simple lancher java plugin to start Hyperstack Stitcher application
 */
public class IMT extends Thread implements PlugIn {
	/** The started Hyperstack Stitcher application */
	protected Process process;
	
	/** Empty setup */
	public void setup() {
		process = null;
	}
	
	/**
	 * Kills the started stitcher process.
	 */
	private void killProcess() {
		try {
			if (process != null) {
				process.destroy();
				process = null;
			}
		} catch (Exception exc) {
			// Swallow exception
		}
	}
	
	/**
	 * Thread.run method to kill the started process.
	 */
	public void run() {
		killProcess();
	}

	/**
	 * PlugIn.run method to start the plugin.
	 */
	public void run(String arg) {
		BufferedInputStream reader = null;
		BufferedOutputStream writer = null;

		Calibration calibration = null;
		double displayRangeMin = 0.0;
		double displayRangeMax = 0.0;

		// Start the stitcher application and sends hyperstack to it.
		try {
			ImagePlus image = WindowManager.getCurrentImage();

			// Check the image
			if (image == null || (image.getBitDepth() != 8 && image.getBitDepth() != 16)) {
				IJ.showMessage("Please select an 8 or 16 bit hyperstack.");
				return;
			}

			// Store calibration and ranges
			calibration = image.getCalibration();
			displayRangeMin = image.getDisplayRangeMin();
			displayRangeMax = image.getDisplayRangeMax();

			// Start process
			//process = Runtime.getRuntime().exec(IJ.getDirectory("plugins") + "/HyperstackStitcher.exe");

			process = Runtime.getRuntime().exec(IJ.getDirectory("plugins") + "/IMT.exe");
			// Add current thread to shutdown hook. The stitcher process will be terminated when ImageJ exits.
			Runtime.getRuntime().addShutdownHook(this);

			// Reader and writer
			reader = new BufferedInputStream(process.getInputStream());
			writer = new BufferedOutputStream(process.getOutputStream());
			
			// Read the "connected" message from stitcher
			readLine(reader);

			// Serialize and send header
			Map<String, String> map = new HashMap<String, String>();
			map.put("Title", image.getTitle());
			map.put("BitDepth", "" + image.getBitDepth());
			map.put("Width", "" + image.getWidth());
			map.put("Height", "" + image.getHeight());
			map.put("Channels", "" + image.getNChannels());
			map.put("Slices", "" + image.getNSlices());
			map.put("Frames", "" + image.getNFrames());
			writeLine(writer, mapToString(map));

			// Send bitmaps
			for (int i = 1; i <= image.getStackSize(); ++i) {
				ImageProcessor imageProcessor = image.getStack().getProcessor(i);

				if (image.getBitDepth() == 8) {
					// Send 8 bit bitmaps
					writeArray(writer, (byte[]) imageProcessor.getPixels());
				} else {
					// Send 16 bit bitmaps
					ByteBuffer buffer = ByteBuffer.allocate(imageProcessor.getPixelCount() * 2);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					buffer.asShortBuffer().put((short[]) imageProcessor.getPixels());

					writeArray(writer, buffer.array());
				}
			}
		} catch (Exception e) {
			// An error occurred
			IJ.showMessage("Failed to send hyperstack to Stitcher.\n" + e.toString());
		}

		// Wait for result hyperstacks from sticher process
		try {
			do {
				// Read header
				String header = readLine(reader);

				// Stops in case of empty header
				if ("".equals(header)) {
					break;
				}

				// Deserialize the header
				Map<String, String> map = stringToMap(header);
				int bitDepth = Integer.parseInt(map.get("BitDepth"));
				int width = Integer.parseInt(map.get("Width"));
				int height = Integer.parseInt(map.get("Height"));
				int channels = Integer.parseInt(map.get("Channels"));
				int slices = Integer.parseInt(map.get("Slices"));
				int frames = Integer.parseInt(map.get("Frames"));
				int size = channels * slices * frames;

				// Create a new hyperstack
				ImagePlus image2 = IJ.createImage(map.get("Title"), width, height, size, bitDepth);
				ImageStack stack2 = image2.getStack();
				for (int i = 0; i < size; ++i) {
					byte[] bytes = readArray(reader);

					if (bitDepth == 8) {
						// Reads 8 bit bitmap
						stack2.setPixels(bytes, i + 1);
					} else {
						// Reads 16 bit bitmap
						short[] shorts = new short[bytes.length / 2];
						ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
						stack2.setPixels(shorts, i + 1);
					}
				}

				// Adjust the parameters of new hyperstack
				image2.setDimensions(channels, slices, frames);
				image2.setDisplayMode(IJ.GRAYSCALE);
				image2.setOpenAsHyperStack(true);
				image2.setDisplayRange(displayRangeMin, displayRangeMax);
				image2.setCalibration(calibration);
				image2.show();
			} while (true);
		} catch (Exception e) {
			// An error occurred
			IJ.showMessage("Failed to read hyperstack from Stitcher.\n" + e.toString());
		}

		// Force stop stitcher process
		killProcess();
	}

	/**
	 * Serializes map of strings into a string.
	 * @param map Map of strings to serialize.
	 * @return Serialized string.
	 */
	public static String mapToString(Map<String, String> map) {
		StringBuilder stringBuilder = new StringBuilder();

		for (String key : map.keySet()) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append("&");
			}
			String value = map.get(key);
			try {
				stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
				stringBuilder.append("=");
				stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("This method requires UTF-8 encoding support", e);
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * Deserializes string to map of strings.
	 * @param input String to deserialize.
	 * @return Deserialized map of strings.
	 */
	public static Map<String, String> stringToMap(String input) {
		Map<String, String> map = new HashMap<String, String>();

		String[] nameValuePairs = input.split("&");
		for (String nameValuePair : nameValuePairs) {
			String[] nameValue = nameValuePair.split("=");
			try {
				map.put(URLDecoder.decode(nameValue[0], "UTF-8"),
						nameValue.length > 1 ? URLDecoder.decode(nameValue[1], "UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("This method requires UTF-8 encoding support", e);
			}
		}

		return map;
	}

	/**
	 * Reads array from stream.
	 * @param reader BufferedInputStream to read.
	 * @return The read byte array.
	 * @throws IOException
	 */
	private static byte[] readArray(BufferedInputStream reader) throws IOException {
		// Read length (int32)
		byte[] buffer = new byte[4];
		reader.read(buffer, 0, 4);
		int length = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

		// Read buffer in UTF8
		buffer = new byte[length];
		reader.read(buffer, 0, length);

		return buffer;
	}

	/**
	 * Reads a single line from stream.
	 * @param reader BufferedInputStream to read.
	 * @return The read line.
	 * @throws IOException
	 */
	private static String readLine(BufferedInputStream reader) throws IOException {
		// Convert from UTF8 to string
		return new String(readArray(reader), StandardCharsets.UTF_8);
	}

	/**
	 * Writes byte array to stream.
	 * @param writer BufferedOutputStream to write.
	 * @param buffer The array to write.
	 * @throws IOException
	 */
	private static void writeArray(BufferedOutputStream writer, byte[] buffer) throws IOException {
		// Write length (int32)
		writer.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(buffer.length).array(), 0, 4);

		// Write buffer in UTF8
		writer.write(buffer, 0, buffer.length);
	}

	/**
	 * Writes a line to stream.
	 * @param writer BufferedOutputStream to write.
	 * @param string The line to write.
	 * @throws IOException
	 */
	private static void writeLine(BufferedOutputStream writer, String string) throws IOException {
		// Convert to UTF8
		writeArray(writer, string.getBytes(StandardCharsets.UTF_8));
	}
}
