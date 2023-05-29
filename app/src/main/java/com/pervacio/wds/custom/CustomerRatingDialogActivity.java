package com.pervacio.wds.custom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.DashboardLog;

public class CustomerRatingDialogActivity extends AppCompatActivity {

    String rating = "Excellent";
    boolean dontCheckEndFeature = false;
    boolean isDataWipeEnabled = false;
    String ratingType = "Customer";
    String[] resonList;
    TextView cust_rate_head, cust_rate_msg;
    String customerRating = "";
    String customerRatingReason = "";
    String ratingReason = "";
    AppCompatSpinner reasonSpinner;
    AlertDialog wipeConfirmDialog;
    private RadioGroup mRadioGroup;
    private Button mSubmitBtn;
    private RelativeLayout spinnerLayout;
    private TextView txtSelectedItem;
    private int selectedPosition = -1;
    private FrameLayout spinnerFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_rating_dialog);
        setFinishOnTouchOutside(false);
        dontCheckEndFeature = getIntent().getBooleanExtra("dontCheckEndFeature", false);
        ratingType = getIntent().getStringExtra("RatindType");
        txtSelectedItem = findViewById(R.id.txtSelectedItem);
        txtSelectedItem.setText(R.string.reason_for_rating);
        mRadioGroup = (RadioGroup) findViewById(R.id.rating_layout);
        spinnerFrame = (FrameLayout) findViewById(R.id.spinner_frame);
        spinnerFrame.setVisibility(View.VISIBLE);

        resonList = getResources().getStringArray(R.array.wds_reasons_list);
        cust_rate_head = (TextView) findViewById(R.id.cust_rate_head);
        cust_rate_msg = (TextView) findViewById(R.id.cust_rate_msg);
        if ("Agent".equalsIgnoreCase(ratingType)) {
            customerRating = getIntent().getStringExtra("CustomerRating");
            customerRatingReason = getIntent().getStringExtra("CustomerRatingReason");
            cust_rate_head.setText(R.string.agent_satisfy_title);
            cust_rate_msg.setText(R.string.agent_satisfy_msg);
        }
        reasonSpinner = (AppCompatSpinner) findViewById(R.id.reason_spinner);
        reasonSpinner.setVisibility(View.GONE);
        spinnerLayout = (RelativeLayout) findViewById(R.id.spinner_layout);
        spinnerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //selectedPosition = -1;
                //txtSelectedItem.setText(R.string.reason_for_rating);
                reasonSpinner.setVisibility(View.VISIBLE);
                reasonSpinner.performClick();
            }
        });

        reasonSpinner.setDropDownWidth((int) getResources().getDimension(R.dimen.rating_spinner_width));

        MyCustomAdapter myCustomAdapter = new MyCustomAdapter(CustomerRatingDialogActivity.this
                , R.layout.spinner_custom_text_view
                , resonList);

        reasonSpinner.setAdapter(myCustomAdapter);

        AdapterView.OnItemSelectedListener reasonSelectedListener = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> spinner, View container,
                                       int position, long id) {
                selectedPosition = position;
                txtSelectedItem.setText(resonList[position]);
                DLog.log("Text cnv= " + resonList[position]);

                //reasonSpinner.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        };
        reasonSpinner.setOnItemSelectedListener(reasonSelectedListener);
        //reasonSpinner.setSelection(0);

        mSubmitBtn = findViewById(R.id.submit_btn);

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                if (selectedId == R.id.bad_rb) {
                    spinnerLayout.setVisibility(View.VISIBLE);
                    //selectedPosition = -1;
                    //txtSelectedItem.setText(R.string.reason_for_rating);
                } else
                    spinnerLayout.setVisibility(View.GONE);
            }
        });
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSubmitBtn.setEnabled(false);
                int checkedId = mRadioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.excellent_rb) {
                    rating = "Excellent";
                } else if (checkedId == R.id.good_rb) {
                    rating = "Good";
                } else if (checkedId == R.id.bad_rb) {
                    rating = "Bad";
                }
                //CommandServer.getInstance(CustomerRatingDialogActivity.this).postEventData("CUSTOMER_RATING", rating);
                if ("Agent".equalsIgnoreCase(ratingType)) {
                    String ratingReason = "";
                    if (selectedPosition != -1)
                        ratingReason = resonList[selectedPosition];
                    String finalRating = "CustomerRating=" + customerRating;
                    if ("Bad".equalsIgnoreCase(customerRating) && customerRatingReason != null && !"".equalsIgnoreCase(customerRatingReason))
                        finalRating += "\n" + "CustomerRatingReason=" + customerRatingReason;
                    finalRating += "\n" + "AgentRating=" + rating;
                    if ("Bad".equalsIgnoreCase(rating) &&  ratingReason != null && !"".equalsIgnoreCase(ratingReason))
                        finalRating += "\n" + "AgentRatingReason=" + ratingReason;
                    DLog.log("position= " + selectedPosition);
                    DLog.log("finalRating= " + finalRating);
                    DashboardLog.getInstance().addAdditionalInfo(finalRating);
                    DashboardLog.getInstance().updateToServer(true);
/*                    if (FeatureConfig.getInstance().getProductConfig().isDataWipeEnabled()) {
                        showWipeConfirmDialog();
                    } else if (FeatureConfig.getInstance().getProductConfig().isAccAndPinRemovalEnabled()) {
                        showAccLockConfirmDialog();
                    } else {*/
                    Intent intent = new Intent(CustomerRatingDialogActivity.this, ThankYouActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    //}
                } else {
                    String ratingReason = "";
                    if (selectedPosition != -1)
                        ratingReason = resonList[selectedPosition];
                    DLog.log("CustomerRating= " + rating);
                    Intent agentIntent = new Intent(getApplicationContext(), CustomerRatingDialogActivity.class);
                    //agentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    agentIntent.putExtra("CustomerRating", rating);
                    agentIntent.putExtra("CustomerRatingReason", ratingReason);
                    agentIntent.putExtra("RatindType", "Agent");
                    startActivity(agentIntent);
                    finish();
                }
            }
        });
    }

    public class MyCustomAdapter extends ArrayAdapter<String> {

        public MyCustomAdapter(Context context, int textViewResourceId,
                               String[] objects) {
            super(context, textViewResourceId, objects);
            // TODO Auto-generated constructor stub
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {
            // TODO Auto-generated method stub
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.custom_spinner_row, parent, false);
            RelativeLayout relMain = (RelativeLayout) row.findViewById(R.id.relMain);
            TextView label = (TextView) row.findViewById(R.id.spinnerText);
            label.setText(resonList[position]);
            return row;
        }
    }
}
