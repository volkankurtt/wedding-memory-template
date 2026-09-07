package com.dugunanisi.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dugunanisi.config.AppProperties;

@Component
public class ImageMagickImageConverter implements ImageConverter {

	static final int MAX_EDGE_PX = 1920;
	static final float JPEG_QUALITY = 0.85f;
	private static final int MAGICK_TIMEOUT_SECONDS = 45;

	private static final Logger log = LoggerFactory.getLogger(ImageMagickImageConverter.class);

	private final String magickCommand;

	public ImageMagickImageConverter(AppProperties properties) {
		this.magickCommand = properties.getImageMagick().getCommand();
	}

	@Override
	public byte[] toDisplayJpeg(byte[] original, DetectedImageType type) {
		return toDisplayJpeg(original, type, null);
	}

	@Override
	public byte[] toDisplayJpeg(byte[] original, DetectedImageType type, String originalFileName) {
		if (type == DetectedImageType.JPEG || type == DetectedImageType.PNG || type == DetectedImageType.WEBP) {
			try {
				byte[] jpeg = resizeWithImageIo(original, type, originalFileName);
				if (type != DetectedImageType.JPEG) {
					log.info("ImageIO display jpeg ready type={} bytes={}", type, jpeg.length);
				}
				return jpeg;
			}
			catch (Exception exception) {
				throw new ImageConversionException("Fotoğraf görüntüye dönüştürülemedi.", exception);
			}
		}
		return convertWithMagick(original, type);
	}

	private byte[] convertWithMagick(byte[] original, DetectedImageType type) {
		Path input = null;
		Path output = null;
		Process process = null;
		try {
			input = Files.createTempFile("dugun-in-", type.fileSuffix());
			output = Files.createTempFile("dugun-out-", ".jpg");
			Files.write(input, original);

			ProcessBuilder builder = new ProcessBuilder(
					magickCommand,
					input.toAbsolutePath().toString(),
					"-auto-orient",
					"-resize",
					MAX_EDGE_PX + "x" + MAX_EDGE_PX + ">",
					"-quality",
					"85",
					output.toAbsolutePath().toString());
			builder.redirectErrorStream(true);
			log.info("Starting ImageMagick command={} type={}", magickCommand, type);
			process = builder.start();
			drainAsync(process.getInputStream());
			boolean finished = process.waitFor(MAGICK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new ImageConversionException("Görsel dönüştürme zaman aşımına uğradı.");
			}
			if (process.exitValue() != 0) {
				throw new ImageConversionException("Görsel JPEG'e dönüştürülemedi.");
			}
			byte[] jpeg = Files.readAllBytes(output);
			if (!isJpeg(jpeg)) {
				throw new ImageConversionException("Dönüştürme sonucu geçerli bir JPEG üretmedi.");
			}
			log.info("ImageMagick conversion finished type={} bytes={}", type, jpeg.length);
			return jpeg;
		}
		catch (ImageConversionException exception) {
			throw exception;
		}
		catch (IOException exception) {
			throw new ImageConversionException(
					"Sunucuda ImageMagick (magick) yok veya çalıştırılamadı. JPEG/PNG deneyin veya magick kurun.",
					exception);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ImageConversionException("Görsel dönüştürme kesildi.", exception);
		}
		finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
			deleteQuietly(input);
			deleteQuietly(output);
		}
	}

	static byte[] resizeWithImageIo(byte[] original, DetectedImageType type) throws IOException {
		return resizeWithImageIo(original, type, null);
	}

	static byte[] resizeWithImageIo(byte[] original, DetectedImageType type, String originalFileName) throws IOException {
		long started = System.nanoTime();
		BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
		if (source == null) {
			throw new IOException("ImageIO could not read image");
		}
		int originalWidth = source.getWidth();
		int originalHeight = source.getHeight();
		int orientation = JpegExifOrientation.NORMAL;
		if (type == DetectedImageType.JPEG) {
			orientation = JpegExifOrientation.read(original);
			source = applyExifOrientation(source, orientation);
		}
		int orientedWidth = source.getWidth();
		int orientedHeight = source.getHeight();
		int width = orientedWidth;
		int height = orientedHeight;
		int longest = Math.max(width, height);
		int targetW = width;
		int targetH = height;
		if (longest > MAX_EDGE_PX) {
			double scale = MAX_EDGE_PX / (double) longest;
			targetW = Math.max(1, (int) Math.round(width * scale));
			targetH = Math.max(1, (int) Math.round(height * scale));
		}
		BufferedImage rgb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgb.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, targetW, targetH);
			graphics.drawImage(source, 0, 0, targetW, targetH, null);
		}
		finally {
			graphics.dispose();
		}
		byte[] jpeg = writeJpeg(rgb);
		if (type == DetectedImageType.JPEG) {
			log.info(
					"JPEG display file={} contentType={} orientation={} original={}x{} afterOrientation={}x{} display={}x{} displayBytes={} totalMs={}",
					originalFileName,
					type.contentType(),
					orientation,
					originalWidth,
					originalHeight,
					orientedWidth,
					orientedHeight,
					targetW,
					targetH,
					jpeg.length,
					elapsedMs(started));
		}
		return jpeg;
	}

	private static long elapsedMs(long startedNanos) {
		return (System.nanoTime() - startedNanos) / 1_000_000L;
	}

	static BufferedImage applyExifOrientation(BufferedImage source, int orientation) {
		return switch (orientation) {
			case 2 -> flipHorizontal(source);
			case 3 -> rotate180(source);
			case 4 -> flipVertical(source);
			case 5 -> rotate90CounterClockwise(flipHorizontal(source));
			case 6 -> rotate90Clockwise(source);
			case 7 -> rotate90Clockwise(flipHorizontal(source));
			case 8 -> rotate90CounterClockwise(source);
			default -> source;
		};
	}

	private static BufferedImage rotate90Clockwise(BufferedImage source) {
		BufferedImage dest = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dest.createGraphics();
		try {
			graphics.translate(source.getHeight(), 0);
			graphics.rotate(Math.PI / 2);
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return dest;
	}

	private static BufferedImage rotate90CounterClockwise(BufferedImage source) {
		BufferedImage dest = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dest.createGraphics();
		try {
			graphics.translate(0, source.getWidth());
			graphics.rotate(-Math.PI / 2);
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return dest;
	}

	private static BufferedImage rotate180(BufferedImage source) {
		BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dest.createGraphics();
		try {
			graphics.translate(source.getWidth(), source.getHeight());
			graphics.rotate(Math.PI);
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return dest;
	}

	private static BufferedImage flipHorizontal(BufferedImage source) {
		BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dest.createGraphics();
		try {
			graphics.translate(source.getWidth(), 0);
			graphics.scale(-1, 1);
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return dest;
	}

	private static BufferedImage flipVertical(BufferedImage source) {
		BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dest.createGraphics();
		try {
			graphics.translate(0, source.getHeight());
			graphics.scale(1, -1);
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return dest;
	}

	private static byte[] writeJpeg(BufferedImage image) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		if (param.canWriteCompressed()) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
			writer.setOutput(output);
			writer.write(null, new IIOImage(image, null, null), param);
		}
		finally {
			writer.dispose();
		}
		byte[] jpeg = buffer.toByteArray();
		if (!isJpeg(jpeg)) {
			throw new IOException("JPEG encode failed");
		}
		return jpeg;
	}

	private static boolean isJpeg(byte[] bytes) {
		return bytes != null && bytes.length >= 3
				&& bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
	}

	private static void drainAsync(InputStream stream) {
		Thread thread = new Thread(() -> {
			try {
				stream.readAllBytes();
			}
			catch (IOException ignored) {
				// process closed the stream
			}
		}, "magick-drain");
		thread.setDaemon(true);
		thread.start();
	}

	private static void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ignored) {
			// temp cleanup
		}
	}
}
