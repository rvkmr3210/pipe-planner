package edu.uark.pipeplanparser.util;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;


public class BlockTextExtractionStrategy implements TextExtractionStrategy {
    private final StringBuffer result = new StringBuffer();

    private Vector lastStart;
    private Vector lastEnd;

    @Override
    public String getResultantText() {
        return result.toString();
    }

    @Override
    public void beginTextBlock() {

    }

    public void appendTextChunk(String text) {
        result.append(text);
    }

    @Override
    public void renderText(TextRenderInfo renderInfo) {
        boolean firstRender = result.length() == 0;
        boolean hardReturn = false;

        LineSegment segment = renderInfo.getBaseline();
        Vector start = segment.getStartPoint();
        Vector end = segment.getEndPoint();

        if (!firstRender){
            Vector x0 = start;
            Vector x1 = lastStart;
            Vector x2 = lastEnd;

            // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
            float dist = (x2.subtract(x1)).cross((x1.subtract(x0))).lengthSquared() / x2.subtract(x1).lengthSquared();

            float sameLineThreshold = 1f; // we should probably base this on the current font metrics, but 1 pt seems to be sufficient for the time being
            if (dist > sameLineThreshold)
                hardReturn = true;

            // Note:  Technically, we should check both the start and end positions, in case the angle of the text changed without any displacement
            // but this sort of thing probably doesn't happen much in reality, so we'll leave it alone for now
        }

        if (hardReturn){
            //System.out.println("<< Hard Return >>");
            appendTextChunk("\n");
        } else if (!firstRender){
            if (result.charAt(result.length()-1) != ' ' && renderInfo.getText().length() > 0 && renderInfo.getText().charAt(0) != ' '){ // we only insert a blank space if the trailing character of the previous string wasn't a space, and the leading character of the current string isn't a space
                float spacing = lastEnd.subtract(start).length();
                if (spacing > renderInfo.getSingleSpaceWidth()/2f){
                    appendTextChunk(" ");
                    //System.out.println("Inserting implied space before '" + renderInfo.getText() + "'");
                }
            }
        } else {
            //System.out.println("Displaying first string of content '" + text + "' :: x1 = " + x1);
        }

        //System.out.println("[" + renderInfo.getStartPoint() + "]->[" + renderInfo.getEndPoint() + "] " + renderInfo.getText());
        appendTextChunk(renderInfo.getText());

        lastStart = start;
        lastEnd = end;
    }

    @Override
    public void endTextBlock() {

    }

    @Override
    public void renderImage(ImageRenderInfo renderInfo) {

    }
}
