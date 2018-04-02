package edu.uark.pipeplanparser.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uark.pipeplanparser.model.PipeSegment;

public class PdfUtil {

    @NonNull
    public static ArrayList<String> parsePdf(@NonNull String filename) throws IOException {
        PdfReader reader = new PdfReader(filename);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        ArrayList<String> results = new ArrayList<>();

        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new BlockTextExtractionStrategy());
            String[] split = strategy.getResultantText().split("\n");

            for (String s : split)
                results.add(s);
        }
        reader.close();

        return results;
    }

    @NonNull
    public static ArrayList<PipeSegment> getHoleResultsFromFile(@NonNull String filename) throws IOException {
        ArrayList<String> results = parsePdf(filename);

        ArrayList<PipeSegment> pipeSegmentResults = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            String line = results.get(i);

            if (isPipeLine(line)) {
                ArrayList<String> pipeInformation = splitToList(line);
                pipeInformation.add(results.get(++i));
                pipeInformation.add(results.get(++i));
                pipeInformation.add(results.get(++i));

                pipeSegmentResults.add(new PipeSegment(pipeInformation));
            }
        }

        logPipeResults(pipeSegmentResults);

        return pipeSegmentResults;
    }

    private static void logPipeResults(@NonNull ArrayList<PipeSegment> pipeSegmentResults) {
        String output = "";

        for (PipeSegment p : pipeSegmentResults) {
            output += p.toString() + "\n";
        }

        Log.d("PdfUtil", "Pipe results count: " + pipeSegmentResults.size());
        Log.d("PdfUtil", output);
    }

    private static boolean isFloatingPoint(@NonNull String input) {
        Pattern p = Pattern.compile("[+-]([0-9]*[.])?[0-9]+");
        Matcher m = p.matcher("+" + input);

        return m.matches() && input.contains(".");
    }

    private static boolean isPipeLine(@NonNull String line) {
        String[] array = line.split(" ");
        return isFloatingPoint(array[0]) && line.contains("-") && array.length > 4;
    }

    private static ArrayList<String> splitToList(@NonNull String line) {
        String[] array = line.split(" ");
        ArrayList<String> list = new ArrayList<>();

        Collections.addAll(list, array);

        return list;
    }


}
