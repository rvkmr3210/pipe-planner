package edu.uark.pipeplanparser;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import com.dropbox.chooser.android.DbxChooser;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.uark.pipeplanparser.activity.MapsActivity;
import edu.uark.pipeplanparser.model.PipePlanner;
import edu.uark.pipeplanparser.util.SnackbarUtil;

public class MainActivity extends AppCompatActivity {

    private final int DBX_CHOOSER_REQUEST = 120;

    BroadcastReceiver mReceiver;
    private long mEnqueue;
    private DownloadManager mDownloadManager;

    TableLayout tbPipeDetails;


    TextView mLabelText;
    ArrayList<String> holeSizelist;
    ArrayList<String> holeSizelistdefault = new ArrayList<>();
    ProgressDialog mDownloadDialog;
    ArrayList<String> pipeSizeList = new ArrayList<>();
    ArrayAdapter<String> aa;
    ArrayAdapter<String> aa1;
    ArrayAdapter<String> aa2;
    ArrayAdapter<String> aa3;
    ArrayAdapter<String> aa4;

    EditText etFarmerName, etFieldName, etHoleSpacing;
    Button btUploadPdf, btPrintPipe;

    private List<PipePlanner> pipePlannerList;
    Button btAddRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        holeSizelistdefault.add("supply");
        holeSizelistdefault.add(" 1/4");
        holeSizelistdefault.add("5/16");
        holeSizelistdefault.add("3/8");
        holeSizelistdefault.add("7/16");
        holeSizelistdefault.add("1/2");
        holeSizelistdefault.add("9/16");
        holeSizelistdefault.add("5/8");
        holeSizelistdefault.add("11/16");
        holeSizelistdefault.add("3/4");
        holeSizelistdefault.add("13/16");
        holeSizelistdefault.add(" 7/8");
        holeSizelistdefault.add("15/16");
        holeSizelistdefault.add("1");
        holeSizelistdefault.add("build up");
        initViews();
//        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_results);

        pipePlannerList = new ArrayList<>();

        mLabelText = (TextView) findViewById(R.id.text_info);


        aa = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, holeSizelistdefault);
        aa1 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, holeSizelistdefault);
        aa2 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, holeSizelistdefault);
        aa3 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, holeSizelistdefault);
        aa4 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, holeSizelistdefault);

        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aa1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aa2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aa3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aa4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        activityMainBinding.listholesize.setAdapter(aa);
//        activityMainBinding.spinner.setAdapter(aa1);
//        activityMainBinding.spinner2.setAdapter(aa2);
//        activityMainBinding.spinner3.setAdapter(aa3);
//        activityMainBinding.spinner4.setAdapter(aa4);
        registerReceiver();

          /*  activityMainBinding.uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DbxChooser mChooser = new DbxChooser(getString(R.string.dropbox_key));

                    mChooser.forResultType(DbxChooser.ResultType.PREVIEW_LINK)
                            .launch((MainActivity)view.getContext(), DBX_CHOOSER_REQUEST);
                }
            });*/


    }

    private void initViews() {
        tbPipeDetails = findViewById(R.id.tb_pipe_details);
        btAddRow = findViewById(R.id.bt_add_row);
        etFarmerName = findViewById(R.id.et_farmer_name);
        etFieldName = findViewById(R.id.et_field_name);
        etHoleSpacing = findViewById(R.id.et_hole_spacing);
        for (int i = 0; i < 7; i++) {
            View child = getLayoutInflater().inflate(R.layout.pipe_details_row_layout, null);
            Spinner spHoleSize = child.findViewById(R.id.sp_hole_size);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, holeSizelistdefault);
            spHoleSize.setAdapter(adapter);
            tbPipeDetails.addView(child);
        }

        btAddRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View child = getLayoutInflater().inflate(R.layout.pipe_details_row_layout, null);
                Spinner spHoleSize = child.findViewById(R.id.sp_hole_size);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, holeSizelistdefault);
                spHoleSize.setAdapter(adapter);
                tbPipeDetails.addView(child);
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DBX_CHOOSER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    DbxChooser.Result result = new DbxChooser.Result(data);

                    // http://dropbox.com/sdfasd/Pipe Plan.pdf" as an example
                    if (!result.getName().endsWith(".pdf")) {
                        SnackbarUtil.displaySnackbar(this, R.string.error_bad_pdf);
                        return;
                    }

                    String downloadLink = result.getLink().toString();
                    downloadLink = downloadLink.substring(0, downloadLink.length() - 1) + "1";

                    mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadLink));

                    request.setDestinationInExternalPublicDir("/Pipe Plan Parser", result.getName());
                    mEnqueue = mDownloadManager.enqueue(request);

                    mDownloadDialog = new ProgressDialog(this);
                    mDownloadDialog.setMessage(getString(R.string.dialog_downloading, result.getName()));
                    mDownloadDialog.show();
                } catch (Exception e) {
                    String output = e.getMessage() + "\n";
                    for (StackTraceElement s : e.getStackTrace()) {
                        output += s.toString();
                    }
                    mLabelText.setText(output);
                }
            } else {
                SnackbarUtil.displaySnackbar(this, R.string.error_download);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public void upload(View v) {
        if (v.getId() == R.id.upload_btn) {
            DbxChooser mChooser = new DbxChooser(getString(R.string.dropbox_key));

            mChooser.forResultType(DbxChooser.ResultType.PREVIEW_LINK)
                    .launch(this, DBX_CHOOSER_REQUEST);
        }
    }

    public void printSkip(View v) {
        if (v.getId() == R.id.button_skip) {
            int rowCount = tbPipeDetails.getChildCount();
            ArrayList<PipePlanner> pipePlannerList = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) {
                View row = tbPipeDetails.getChildAt(i);
                PipePlanner pipePlanner = new PipePlanner();

                EditText etStationStartPoint = row.findViewById(R.id.et_station_start_point);
                EditText etStationEndPoint = row.findViewById(R.id.et_station_end_point);
                EditText etComment = row.findViewById(R.id.et_comment);
                EditText etPipeSize = row.findViewById(R.id.et_pipe_size);
                EditText etGps = row.findViewById(R.id.et_gps);
                Spinner spHoleSize = row.findViewById(R.id.sp_hole_size);

                pipePlanner.setStationStart(etStationStartPoint.getText().toString());
                pipePlanner.setStationEnd(etStationEndPoint.getText().toString());
                pipePlanner.setComment(etComment.getText().toString());
                pipePlanner.setPipeSize(etPipeSize.getText().toString());
                pipePlanner.setHoleSize(spHoleSize.getSelectedItem().toString());
                pipePlannerList.add(pipePlanner);
            }
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putParcelableArrayListExtra("PIPE_DETAILS", pipePlannerList);

            startActivity(intent);
        }
    }

    public void registerReceiver() {

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(mEnqueue);
                    Cursor c = mDownloadManager.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            // TODO Figure out how to get the file name from the Cursor object
                            String uriFileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                            try {
                                String parsedText = "";
                                PdfReader reader = new PdfReader(uriFileName);
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
                                etFarmerName.setText(farmname);
                                etFieldName.setText(fieldName);
                                etHoleSpacing.setText(holeSpacing);
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
                                mDownloadDialog.hide();

                                String holesize = StringUtils.substringBetween(parsedText, "Height (ft)", "Use");

                                String[] words = holesize.split("\\r?\\n|\\r");
                                pipeSizeList = new ArrayList<>();
                                for (String data : words) {
                                    PipePlanner pipePlanner = new PipePlanner();
                                    String furrowCount = "", pipeSize = "", pipeFunction = "", holeSize = "", stationStart = "", stationEnd = "", comment = "";
                                    if (data.toLowerCase().contains("build")
                                            || data.toLowerCase().contains("supply")
                                            || (data.toLowerCase().contains("irrigation") && data.contains("-"))) {
                                        if (data.toLowerCase().contains("supply")) {
                                            pipeSize = data.split(" ")[0];
                                            pipeFunction = (data.split(" ")[1]);
                                            holeSize = "Supply";
                                            stationStart = (StringUtils.substringBetween(data, data.split(" ")[1], "-").trim());
                                            stationEnd = (StringUtils.substringBetween(data, "-", "ft"));
                                            comment = " ";
                                            furrowCount = "";
                                        } else if (data.toLowerCase().contains("build")) {
                                            pipeSize = "";
                                            holeSize = "Build Up";
                                            pipeFunction = "Build Up";
                                            String end = data.split(" ")[1];
                                            stationStart = (StringUtils.substringBetween(data, end, "ft").trim());
                                            stationEnd = ("");
                                            comment = (data.split(" ")[(data.split(" ").length) - 1]);
                                            furrowCount = "";

                                        } else {
                                            pipeSize = (data.split(" ")[0]);
                                            pipeFunction = (data.split(" ")[1]);
                                            holeSize = data.split(" ")[(data.split(" ").length)-2];
                                            stationStart = (StringUtils.substringBetween(data, data.split(" ")[1], "-").trim());
                                            stationEnd = (StringUtils.substringBetween(data, "-", "ft"));
                                            comment = (" ");
                                            furrowCount = data.split(" ")[(data.split(" ").length - 1)];

//                                        pipeSizeList.add(data.replaceAll(" .+$", ""));
                                        }
                                        pipePlanner.setComment(comment);
                                        pipePlanner.setStationEnd(stationEnd);
                                        pipePlanner.setStationStart(stationStart);
                                        pipePlanner.setHoleSize(holeSize);
                                        pipePlanner.setPipeFunction(pipeFunction);
                                        pipePlanner.setPipeSize(pipeSize);
                                        pipePlanner.setFurrowCount(furrowCount);
                                        pipePlannerList.add(pipePlanner);
                                    }

                                }
                                Log.d("data", "" + pipePlannerList.size() + "\n");
                                for (int i = 0; i < pipePlannerList.size(); i++) {
                                    PipePlanner pipePlan = pipePlannerList.get(i);
                                    View row;
                                    if (tbPipeDetails.getChildAt(i + 1) == null) {
                                        row = getLayoutInflater().inflate(R.layout.pipe_details_row_layout, null);
                                        tbPipeDetails.addView(row);
                                    }
                                    row = tbPipeDetails.getChildAt(i + 1);
                                    int holeIndex = holeSizelistdefault.indexOf(pipePlan.getHoleSize());

                                    EditText etStationStartPoint = row.findViewById(R.id.et_station_start_point);
                                    EditText etStationEndPoint = row.findViewById(R.id.et_station_end_point);
                                    EditText etComment = row.findViewById(R.id.et_comment);
                                    EditText etPipeSize = row.findViewById(R.id.et_pipe_size);
                                    EditText etGps = row.findViewById(R.id.et_gps);
                                    Spinner spHoleSize = row.findViewById(R.id.sp_hole_size);

                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, holeSizelistdefault);
                                    spHoleSize.setAdapter(adapter);

                                    etStationStartPoint.setText(pipePlan.getStationStart());
                                    etStationEndPoint.setText(pipePlan.getStationEnd());
                                    etComment.setText(pipePlan.getComment());
                                    etPipeSize.setText(pipePlan.getPipeSize());
                                    for (int index = 0; index < holeSizelistdefault.size(); index++) {
                                        String pphs = pipePlan.getHoleSize().trim();
                                        if (pphs.equalsIgnoreCase(holeSizelistdefault.get(index))) {
                                            spHoleSize.setSelection(index);
                                        }
                                    }

                                }

                                ArrayList<String> holeSizelist1 = new ArrayList<>();

                                for (int i = 0; i < words.length; i++) {
                                    if (words.equals("12*10")) {

                                    } else {
                                        if (words[i].equals("")) {
                                            holeSizelist1.add("");

                                        } else {
                                            int index = words[i].indexOf(" ");
                                            String str1 = words[i].substring(index);
                                            holeSizelist1.add(str1.trim().replaceAll(" .+$", ""));
                                        }

                                    }

                                }

                                holeSizelist = new ArrayList<>();

                                for (int i = 0; i < words.length; i++) {
                                    if (words[i].equals("12x10")) {

                                    } else {
                                        if (words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", "").equals("Build") ||
                                                words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", "").equals("12x10")) {

                                        } else {

                                            holeSizelist.add(words[i].replaceAll("^(\\S*\\s){6}", "").replaceAll(" .+$", ""));

                                        }

                                    }

                                }
                                holeSizelist.add("");
//                                /*FOR DYNAMIC SPINNER*/
//                                activityMainBinding.linearLayoutNormal.setVisibility(View.GONE);
//                                activityMainBinding.dynamicSpinner.removeAllViews();
                                /*FOR DYNAMIC SPINNER*/
                                for (int i = 0; i < pipeSizeList.size(); i++) {
                                    Spinner spinner = new Spinner(MainActivity.this);
                                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
                                            R.layout.spinnerxml, R.id.weekofday, holeSizelistdefault);
                                    //dataAdapter.setDropDownViewResource(android.R.layout.simple_list_item_2);
                                    spinner.setAdapter(dataAdapter);
//                                    activityMainBinding.dynamicSpinner.addView(spinner);
                                    for (int index = 0; index < holeSizelistdefault.size(); index++) {
                                        if (holeSizelist.get(i).equalsIgnoreCase(holeSizelistdefault.get(index))) {
                                            //spinner.getSelectedItemPosition(index);
                                            spinner.setSelection(index);

                                            // int spinnerPosition = aa.getPosition(holeSizelistdefault.get(index));
                                            //activityMainBinding.listholesize.setSelection(spinnerPosition);

                                        }
                                    }
                                }


/*
                                // Iterate over the elements of the first list.
                                for (int index = 0; index < holeSizelistdefault.size(); index++)
                                { Spinner spinner = new Spinner(MainActivity.this);
                                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
                                            R.layout.spinnerxml, R.id.weekofday,holeSizelistdefault);
                                    //dataAdapter.setDropDownViewResource(android.R.layout.simple_list_item_2);
                                    spinner.setAdapter(dataAdapter);
                                    activityMainBinding.dynamicSpinner.addView(spinner);
                                    // Check if the element is also in the second list.
                                   *//* if (holeSizelist.contains(holeSizelistdefault.get(index)))
                                    {


                                        int spinnerPosition = aa.getPosition(holeSizelistdefault.get(index));
                                        activityMainBinding.listholesize.setSelection(spinnerPosition);

                                    }*//*
                                }*/

                                PipeSizeAdapter myListAdapter = new PipeSizeAdapter(MainActivity.this, pipeSizeList);
//                                activityMainBinding.listMain.setAdapter(myListAdapter);

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // start our viewer activity
                            /*    Intent nextActivity = new Intent(MainActivity.this, PipeActivity.class);
                                nextActivity.putExtra(PipeActivity.EXTRA_PDF_FILENAME, uriFileName);
                                startActivity(nextActivity);*/
                        } else {
                            mDownloadDialog.hide();
                            SnackbarUtil.displaySnackbar(MainActivity.this, R.string.error_download);
                        }
                    } else {
                        mDownloadDialog.hide();
                        SnackbarUtil.displaySnackbar(MainActivity.this, R.string.error_download);
                    }
                } else

                {
                    mDownloadDialog.hide();
                    SnackbarUtil.displaySnackbar(MainActivity.this, R.string.error_download);
                }
            }
        }

        ;

        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private class PipeSizeAdapter extends BaseAdapter {

        private Context context;
        ArrayList<String> pipeSizeList;
        private LayoutInflater myInflater;

        LayoutInflater mInflater;

        public PipeSizeAdapter(Context context, ArrayList<String> pipeSizeList) {
            this.context = context;
            this.pipeSizeList = pipeSizeList;
            mInflater = LayoutInflater.from(context);
        }


        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return pipeSizeList.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup arg2) {
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.layout_list, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.caption = (EditText) convertView.findViewById(R.id.editText1);

                // Bind the data efficiently with the holder.

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
            holder.caption.setText(pipeSizeList.get(position));

           /* holder.caption.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before,
                                          int count) {
                   *//* final int position2 = holder.caption.getId();
                    final EditText Caption = (EditText) holder.caption;
                    if(Caption.getText().toString().length()>0){
                        list.set(position2,Integer.parseInt(Caption.getText().toString()));
                    }else{
                        Toast.makeText(context, "Please enter some value", Toast.LENGTH_SHORT).show();
                    }*//*
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }

            });*/

            return convertView;
        }

    }


    static class ViewHolder {
        EditText caption;
    }
}
