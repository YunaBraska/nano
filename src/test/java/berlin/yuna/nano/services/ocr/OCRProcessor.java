package berlin.yuna.nano.services.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.nano.services.ocr.Categorize.executeTasks;

public class OCRProcessor {

    // Auto-detected path
    public static final String TESSERACT_PATH;
    // Auto-detected version
    public static final String TESSERACT_VERSION;
    // Auto-detected installed languages
    public static final String TESSERACT_LANGUAGES;

    protected static OCRMetadata parseText(final Path imagePath) {
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath))
            return null;
        final OCRMetadata metadata = new OCRMetadata();
        final List<Callable<Object>> tasks = List.of(
            Executors.callable(() -> addMetaData(imagePath, metadata)),
            Executors.callable(() -> addTextBlocks(imagePath, metadata))
        );
        executeTasks(tasks);
        return metadata;
    }

    private static void addTextBlocks(final Path imagePath, final OCRMetadata metadata) {
        executeStream(TESSERACT_PATH, imagePath.toString(), "stdout", "-l", TESSERACT_LANGUAGES, "--psm", "1", "tsv")
            .skip(1)
            .forEach(line -> {
                final String[] parts = line.split("\t");
                if (parts.length >= 11) {
                    final TextBlock block = new TextBlock();
                    block.level = Integer.parseInt(parts[0]);
                    block.page = Integer.parseInt(parts[1]);
                    block.block = Integer.parseInt(parts[2]);
                    block.blockPart = Integer.parseInt(parts[3]);
                    block.blockLine = Integer.parseInt(parts[4]);
                    block.blockWord = Integer.parseInt(parts[5]);
                    block.left = Integer.parseInt(parts[6]);
                    block.top = Integer.parseInt(parts[7]);
                    block.width = Integer.parseInt(parts[8]);
                    block.height = Integer.parseInt(parts[9]);
                    block.confidence = Double.parseDouble(parts[10]);
                    block.text = parts.length > 11 ? parts[11] : null;
                    if (block.top != -1 && block.left != -1 && block.height != -1 && block.width != -1 && block.text != null)
                        metadata.textBlocks.add(block);
                }
            });
    }

    private static void addMetaData(final Path imagePath, final OCRMetadata metadata) {
        executeStream(TESSERACT_PATH, imagePath.toString(), "stdout", "--psm", "2")
            .map(line -> {
                final int lastSpaceIndex = line.lastIndexOf(' ');
                return lastSpaceIndex > 0
                    ? new String[]{line.substring(0, lastSpaceIndex).toLowerCase(), line.substring(lastSpaceIndex + 1).trim()}
                    : new String[]{line};
            })
            .filter(line -> line.length > 1)
            .forEach(parts -> {
                if (parts[0].contains("resolution")) {
                    metadata.resolution = parts[parts.length - 1];
                } else if (parts[0].contains("orientation")) {
                    metadata.orientation = parts[parts.length - 1];
                } else if (parts[0].contains("direction")) {
                    metadata.writingDirection = parts[parts.length - 1];
                } else if (parts[0].contains("textlineorder")) {
                    metadata.textLineOrder = parts[parts.length - 1];
                } else if (parts[0].contains("angle")) {
                    metadata.angle = parts[parts.length - 1];
                }

            });
    }

    protected static Stream<String> executeStream(final String... command) {
        final List<String> result = new ArrayList<>();
        execute(result::add, command);
        return result.stream();
    }

    protected static void execute(final Consumer<String> lineConsumer, final String... command) {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
            final Process process = processBuilder.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        } catch (final IOException ignored) {
            // ignore
        }
    }

    public static String detectTesseractPath() {
        return Arrays.stream(new String[]{
            "/usr/bin/tesseract",  // Common path for Unix
            "/usr/local/bin/tesseract",  // Homebrew (macOS)
            "/opt/homebrew/bin/tesseract",  // Homebrew on Apple Silicon (macOS)
            "/usr/local/share/tesseract-ocr",  // Some Linux distributions
            "/usr/share/tesseract-ocr",  // Some other Linux distributions
            "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",  // Common path for Windows
            "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe"
        }).filter(OCRProcessor::verifyTesseractPath).findFirst().orElseGet(() -> {
            try {
                final Process process = new ProcessBuilder(System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which", "tesseract").start();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final String path = reader.readLine();
                return path != null && !path.isEmpty() && verifyTesseractPath(path) ? path : null;
            } catch (final Exception ignored) {
                return null;
            }
        });
    }

    protected static boolean verifyTesseractPath(final String pathStr) {
        try {
            final Path path = Path.of(pathStr).toRealPath();
            return Files.exists(path) && Files.isExecutable(path);
        } catch (final IOException ignored) {
            return false;
        }
    }


    public static class OCRMetadata {
        public String resolution;
        public String orientation;
        public String writingDirection;
        public String textLineOrder;
        public String angle;
        public List<TextBlock> textBlocks = new ArrayList<>();

        @Override
        public String toString() {
            return "OCRMetadata{" +
                "resolution='" + resolution + '\'' +
                ", orientation='" + orientation + '\'' +
                ", writingDirection='" + writingDirection + '\'' +
                ", textLineOrder='" + textLineOrder + '\'' +
                ", angle='" + angle + '\'' +
                ", size=" + textBlocks.size() +
                '}';
        }
    }

    public static class TextBlock {
        int level;
        int page;
        int block;
        int blockPart;
        int blockLine;
        int blockWord;
        int left;
        int top;
        int height;
        int width;
        double confidence;
        String text;

        @Override
        public String toString() {
            return "TextBlock{" +
                "level=" + level +
                ", page=" + page +
                ", block=" + block +
                ", blockPart=" + blockPart +
                ", blockLine=" + blockLine +
                ", blockWord=" + blockWord +
                ", left=" + left +
                ", top=" + top +
                ", height=" + height +
                ", width=" + width +
                ", confidence=" + confidence +
                ", text='" + text + '\'' +
                '}';
        }
    }

    static {
        // Auto-detect path, version and installed languages
        TESSERACT_PATH = detectTesseractPath();
        TESSERACT_VERSION = executeStream(TESSERACT_PATH, "--version")
            .map(line -> line.split("\\s"))
            .filter(parts -> parts.length > 1)
            .map(parts -> parts[1])
            .findFirst()
            .orElse(null);
        TESSERACT_LANGUAGES = executeStream(TESSERACT_PATH, "--list-langs")
            .flatMap(line -> Arrays.stream(line.split("\\R")))
            .filter(lang -> !"snum".equals(lang))
            .filter(lang -> !"osd".equals(lang))
            .filter(lang -> !"equ".equals(lang))
            .filter(lang -> lang.length() < 10)
            .filter(lang -> !lang.contains(" "))
            .collect(Collectors.joining("+"));
    }
}
