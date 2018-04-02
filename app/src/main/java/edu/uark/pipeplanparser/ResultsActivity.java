package edu.uark.pipeplanparser;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import edu.uark.pipeplanparser.activity.PipeActivity;
import edu.uark.pipeplanparser.databinding.ActivityResultsBinding;

public class ResultsActivity extends AppCompatActivity {
    ActivityResultsBinding activityMainBinding;
    ArrayList<String> holeSizelist;
    ArrayList<String> pipesizeList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       activityMainBinding = DataBindingUtil.setContentView(this,R.layout.activity_results);
        if(getIntent()!=null)
        {
            activityMainBinding.editText.setText(getIntent().getStringExtra(PipeActivity.farmName));
            activityMainBinding.editText2.setText(getIntent().getStringExtra(PipeActivity.fieldName));
            activityMainBinding.editText3.setText(getIntent().getStringExtra(PipeActivity.holeSpacing));


            holeSizelist = (ArrayList<String>) getIntent().getSerializableExtra(PipeActivity.holesize);
            pipesizeList = (ArrayList<String>) getIntent().getSerializableExtra(PipeActivity.pipesize);
            PipeSizeAdapter myListAdapter = new PipeSizeAdapter();
            ListView listView = (ListView) findViewById(R.id.list_main);
            listView.setAdapter(myListAdapter);
            /*HoleSizeAdapter holesizeAdapter = new HoleSizeAdapter();
            ListView listViewholesize = (ListView) findViewById(R.id.listHolesize);
            listViewholesize.setAdapter(holesizeAdapter);*/
            ArrayAdapter<String> aa = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,holeSizelist);
            Spinner listViewholesize = (Spinner) findViewById(R.id.listHolesize);
            aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //Setting the ArrayAdapter data on the Spinner
            listViewholesize.setAdapter(aa);
        }

    }

    private class PipeSizeAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            // TODO Auto-generated method stub

            return pipesizeList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return pipesizeList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //ViewHolder holder = null;
            final ViewHolder holder;
            if (convertView == null) {

                holder = new ViewHolder();
                LayoutInflater inflater = ResultsActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.layout_list, null);
                holder.editText1 = (EditText) convertView.findViewById(R.id.editText1);

                convertView.setTag(holder);

            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.ref = position;

            holder.editText1.setText(pipesizeList.get(position));
            holder.editText1.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                   // arrTemp[holder.ref] = arg0.toString();
                }
            });

            return convertView;
        }

        private class ViewHolder {
            EditText editText1;
            int ref;
        }


    }
    private class HoleSizeAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            // TODO Auto-generated method stub

            return holeSizelist.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return holeSizelist.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //ViewHolder holder = null;
            final ViewHolder holder;
            if (convertView == null) {

                holder = new ViewHolder();
                LayoutInflater inflater = ResultsActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.layout_list, null);
                holder.editText1 = (EditText) convertView.findViewById(R.id.editText1);

                convertView.setTag(holder);

            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.ref = position;

            holder.editText1.setText(holeSizelist.get(position));
            holder.editText1.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                    // arrTemp[holder.ref] = arg0.toString();
                }
            });

            return convertView;
        }

        private class ViewHolder {
            EditText editText1;
            int ref;
        }


    }

}

