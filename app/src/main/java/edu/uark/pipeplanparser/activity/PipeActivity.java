package edu.uark.pipeplanparser.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;


import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.uark.pipeplanparser.R;
import edu.uark.pipeplanparser.ResultsActivity;
import edu.uark.pipeplanparser.model.Pipe;
import edu.uark.pipeplanparser.model.PipeSegment;

public class PipeActivity extends FragmentActivity
        implements OnMapReadyCallback,
        SeekBar.OnSeekBarChangeListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener {
    String[] pdflist;
    File[] imagelist;
    public static final String TAG = PipeActivity.class.getSimpleName();

    public static final String EXTRA_PDF_FILENAME = "EXTRA_FILENAME";

    private static final int STEP_NOT_STARTED = -1;
    private static final int STEP_ANGLE_SELECT = 0;
    private static final int STEP_SPACING_SELECT = 1;
    private static final int STEP_FINISHED = 2;

    private GoogleMap mMap;
    Pipe mPipe;

    TextView mLabelInstruction;

    TextView mLabelDiameter;
    TextView mLabelPipeLength;
    TextView mLabelHolesPerFurrow;
    TextView mLabelHoleSize;
    TextView mLabelFurrowCount;

    SeekBar mSeekBar;

    Button mCloseButton;
    Button mOkButton;
    Button mConnectButton;

    RelativeLayout mSegmentInfoContainer;

    private int mCurrentStep = STEP_NOT_STARTED;
    public static String farmName = "FARMNAME";
    public static String fieldName = "FieldNAME";
    public static String holeSpacing = "HOLESPACING";
    public static String pipesize = "PIPESIZELIST";
    public static String holesize = "HOLESIZELIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pipe);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mSegmentInfoContainer = (RelativeLayout) findViewById(R.id.segment_info_container);
        mCloseButton = (Button) findViewById(R.id.button_close);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInfoContainerVisibility(View.GONE);
                mPipe.hideMarkerInfoWindows();
            }
        });

        mOkButton = (Button) findViewById(R.id.button_ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Change this to "setupStep(mCurrentStep + 1);" if we decide to change furrow spacing
                setupStep(mCurrentStep + 2);
            }
        });

        mConnectButton = (Button) findViewById(R.id.button_connect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupStep(STEP_FINISHED);
            }
        });

        mLabelInstruction = (TextView) findViewById(R.id.label_instruction);
        mLabelDiameter = (TextView) findViewById(R.id.label_diameter);
        mLabelPipeLength = (TextView) findViewById(R.id.label_pipe_length);
        mLabelHolesPerFurrow = (TextView) findViewById(R.id.label_holes_per_furrow);
        mLabelHoleSize = (TextView) findViewById(R.id.label_hole_size);
        mLabelFurrowCount = (TextView) findViewById(R.id.label_furrow_count);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setMessage(R.string.welcome_message)
                .setPositiveButton("OK", new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setupStep(STEP_ANGLE_SELECT);
                    }
                });
        dialogBuilder.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setOnMarkerClickListener(this);

        String pdfFilename = getIntent().getStringExtra(EXTRA_PDF_FILENAME);

        try {
            String parsedText = "";
            PdfReader reader = new PdfReader(pdfFilename);
            int n = reader.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                parsedText = parsedText + PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\r\n"; //Extracting the content from the different pages
                //  System.out.println("sample"+parsedText);

            }
            reader.close();

            String farmname = StringUtils.substringBetween(parsedText, "Farm Name:", "Uniformity:");
            String uniformity = StringUtils.substringBetween(parsedText, "Uniformity:", "Field Name:");
            String fieldName = StringUtils.substringBetween(parsedText, "Field Name:", "Min. Head Pressure:");
            String holeSpacing = StringUtils.substringBetween(parsedText, "Hole Spacing:", "Set Area:");

           /*   String[] words=holesize.split("\\r?\\n|\\r");;//splits the string based on string


            Log.i(TAG, "onMapReady1: "+farmname);
            Log.i(TAG, "onMapReady2: "+uniformity);

            Intent intent=new Intent(this, ResultsActivity.class);
            intent.putExtra(PipeActivity.farmName,farmname);
            intent.putExtra(PipeActivity.fieldName,fieldName);
            intent.putExtra(PipeActivity.holeSpacing,holeSpacing);
*/
            // startActivity(intent);
               /* PDFParser pdfParser = new PDFParser(pdfFilename);
                pdfParser.parse();
                PDDocument pdDocument = new PDDocument(pdfParser.getDocument());
                PDFTextStripper pdfTextStripper = new PDFTextStripper();
               String string = pdfTextStripper.getText(pdDocument);*/


            String holesize = StringUtils.substringBetween(parsedText, "Height (ft)", "Use");

            String[] words = holesize.split("\\r?\\n|\\r");
            Log.i(TAG, "onMapReady4: " + words);
            ArrayList<String> pipeSizeList = new ArrayList<>();
            for (String data : words) {
                if (data.equals("") || data.contains("Build")) {

                } else
                    pipeSizeList.add(data.replaceAll(" .+$", ""));


            }

            ArrayList<String> holeSizelist1 = new ArrayList<>();

            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("") || words.equals("12*10")) {

                } else {
                    int index = words[i].indexOf(" ");
                    String str1 = words[i].substring(index);
                    holeSizelist1.add(str1.trim().replaceAll(" .+$", ""));
                }

            }

            ArrayList<String> holeSizelist = new ArrayList<>();

            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("") || words[i].equals("12x10")) {

                } else {
                    if (words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", "").equals("Build")||
                            words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", "").equals("12x10")) {

                    } else {
                        holeSizelist.add(words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", ""));

                    }

                }

            }

            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra(PipeActivity.farmName, farmname);
            intent.putExtra(PipeActivity.fieldName, fieldName);
            intent.putExtra(PipeActivity.holeSpacing, holeSpacing);
            intent.putStringArrayListExtra(PipeActivity.pipesize, pipeSizeList);
            intent.putStringArrayListExtra(PipeActivity.holesize, holeSizelist);
            startActivity(intent);
           /* PDFTableExtractor extractor = new PDFTableExtractor();
            List<Table> tables = extractor.setSource(pdfFilename).extract();*/
            /*List<Table> tables = extractor.setSource(“table.pdf”)
                    .addPage(0)
                    .addPage(1)
                    .exceptLine(0) //the first line in each page
                    .exceptLine(1) //the second line in each page
                    .exceptLine(-1)//the last line in each page
                    .extract();*/
            //System.out.println("sample"+title);

           /* List<PipeSegment> pipeSegments = getExampleSegments();
            //List<PipeSegment> pipeSegments = PdfUtil.getHoleResultsFromFile(pdfFilename);
            mPipe = new Pipe(pipeSegments);*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
     /*   //mPipe.addToMap(mMap);

        moveMapToPipes();*/
    }

    public List<PipeSegment> getExampleSegments() {
        List<PipeSegment> pipeSegments = new ArrayList<>();

        LatLng[] latLngs = new LatLng[4];
        latLngs[0] = new LatLng(36.099498, -94.173996);
        latLngs[1] = new LatLng(36.099581, -94.173996);
        latLngs[2] = new LatLng(36.099661, -94.173999);
        latLngs[3] = new LatLng(36.099703, -94.173997);

        PipeSegment pipeSegment1 = new PipeSegment(12.0f, 0, 150, "", 1, "5/8", 60, latLngs[0], latLngs[1]);
        PipeSegment pipeSegment2 = new PipeSegment(12.0f, 150, 300, "", 1, "3/8", 98, latLngs[1], latLngs[2]);
        PipeSegment pipeSegment3 = new PipeSegment(12.0f, 300, 450, "", 1, "1/2", 118, latLngs[2], latLngs[3]);

        pipeSegments.add(pipeSegment1);
        pipeSegments.add(pipeSegment2);
        pipeSegments.add(pipeSegment3);

        return pipeSegments;
    }

    private void moveMapToPipes() {
        LatLngBounds mapBounds = mPipe.getBounds();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 150));
    }

    public void updateDisplayForSegment(PipeSegment segment) {
        setInfoContainerVisibility(View.VISIBLE);
        mOkButton.setVisibility(View.GONE);
        mLabelInstruction.setVisibility(View.GONE);
        mSeekBar.setVisibility(View.GONE);
        mCloseButton.setVisibility(View.VISIBLE);
        mLabelDiameter.setVisibility(View.VISIBLE);
        mLabelDiameter.setText(getString(R.string.label_diameter, segment.getDiameter()));
        mLabelPipeLength.setVisibility(View.VISIBLE);
        mLabelPipeLength.setText(getString(R.string.label_pipe_length, segment.getLengthFrom(), segment.getLengthTo()));
        mLabelFurrowCount.setVisibility(View.VISIBLE);
        mLabelFurrowCount.setText(getString(R.string.label_furrow_count, segment.getFurrowCount()));
        mLabelHoleSize.setVisibility(View.VISIBLE);
        mLabelHoleSize.setText(getString(R.string.label_hole_size, segment.getHoleSize()));
        mLabelHolesPerFurrow.setVisibility(View.VISIBLE);
        mLabelHolesPerFurrow.setText(getString(R.string.label_holes_per_furrow, segment.getHolesPerFurrow()));
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        switch (mCurrentStep) {
            case STEP_ANGLE_SELECT:
            case STEP_SPACING_SELECT:
                return false;
            default:
                if (mPipe != null) {
                    PipeSegment segment = mPipe.segmentForMarker(marker);
                    updateDisplayForSegment(segment);
                }
                break;
        }

        return false;
    }

    private void setupStep(int step) {
        mCurrentStep = step;
        switch (step) {
            case STEP_ANGLE_SELECT:
                setInstructionMessage(R.string.instruction_angle_selection);
                mSeekBar.setVisibility(View.VISIBLE);
                mSeekBar.setMax(360);
                mSeekBar.setProgress(0);
                break;
            case STEP_SPACING_SELECT:
                setInstructionMessage(R.string.instruction_spacing_selection);
                mSeekBar.setVisibility(View.VISIBLE);
                mSeekBar.setMax(9);
                mSeekBar.setProgress(0);
                break;
            case STEP_FINISHED:
                setInfoContainerVisibility(View.GONE);
                mConnectButton.setVisibility(View.VISIBLE);
                Intent connectIntent = new Intent(this, ConnectActivity.class);
                connectIntent.putExtra(ConnectActivity.EXTRA_PIPE, mPipe);
                startActivity(connectIntent);
                break;
            default:
                setInfoContainerVisibility(View.GONE);
        }
    }

    private void setInfoContainerVisibility(int visibility) {
        switch (visibility) {
            case View.INVISIBLE:
            case View.GONE:
                mSegmentInfoContainer.animate().alpha(0).setDuration(250).start();
                mSegmentInfoContainer.setLayoutAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSegmentInfoContainer.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                break;
            case View.VISIBLE:
                mSegmentInfoContainer.setVisibility(View.VISIBLE);
                mSegmentInfoContainer.animate().alpha(1).setDuration(250).start();
                break;
            default:
                break;
        }
    }

    private void setInstructionMessage(@StringRes int stringResource) {
        mLabelInstruction.setText(stringResource);
        mLabelInstruction.setVisibility(View.VISIBLE);
        mLabelHoleSize.setVisibility(View.GONE);
        mLabelFurrowCount.setVisibility(View.GONE);
        mLabelPipeLength.setVisibility(View.GONE);
        mLabelDiameter.setVisibility(View.GONE);
        mLabelHolesPerFurrow.setVisibility(View.GONE);
        mCloseButton.setVisibility(View.GONE);
        mOkButton.setVisibility(View.VISIBLE);
        setInfoContainerVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mPipe != null) {
            switch (mCurrentStep) {
                case STEP_ANGLE_SELECT:
                    mPipe.setAngle(progress);
                    mPipe.drawFurrows(mMap);
                    break;
                case STEP_SPACING_SELECT:
                    // TODO Adjust spacing of lines
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
