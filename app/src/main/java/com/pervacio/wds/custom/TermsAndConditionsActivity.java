package com.pervacio.wds.custom;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.util.Arrays;
import java.util.List;


/**
 * Created by Surya Polasanapalli on 25-05-2017.
 */

public class TermsAndConditionsActivity extends AppCompatActivity {

    PreferenceHelper preferenceHelper;
    private Context mContext;
    private TextView termsConditionsTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        preferenceHelper = PreferenceHelper.getInstance(TermsAndConditionsActivity.this);
//        if (getIntent().getBooleanExtra("isPrivacyPolicyScreen", false))
//            getSupportActionBar().setTitle(getString(R.string.str_privacy_statement));
//        else if(getIntent().getBooleanExtra("isContentPolicyScreen", false))
//        getSupportActionBar().setTitle(getString(R.string.content_policy));
//        else
//            getSupportActionBar().setTitle(getString(R.string.terms_and_condition_dialog_header));
        setContentView(R.layout.activity_terms_and_conditions);
        Button iAgree = (Button) findViewById(R.id.btn_postive);
        Button iDisagree = (Button) findViewById(R.id.btn_negative);
        termsConditionsTv = (TextView) findViewById(R.id.terms_conditions_tv);
        if (getIntent().getBooleanExtra("isPrivacyPolicyScreen", false)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.privacy_policy), Html.FROM_HTML_MODE_COMPACT));
            else
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.privacy_policy)));
        } else if(getIntent().getBooleanExtra("isContentPolicyScreen", false)){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.permissions_policy_body_tf), Html.FROM_HTML_MODE_COMPACT));
            else
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.permissions_policy_body_tf)));
        }
//        termsConditionsTv.setMovementMethod(LinkMovementMethod.getInstance());

        iAgree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killApp();
            }
        });

        iDisagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killApp();
            }
        });
        if (Constants.NEW_PLAYSTORE_FLOW || FeatureConfig.getInstance().getProductConfig().isShowPermissionsInstructions()) {
            iDisagree.setVisibility(View.GONE);
            iAgree.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            EMUtility.makeActionOverflowMenuShown(this);
        }
        if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
            TextView read_more_tv = (TextView) findViewById(R.id.read_more_tv);
            read_more_tv.setVisibility(View.VISIBLE);
            readMoreTsAndCs(read_more_tv);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        List<String> languages = Arrays.asList(FeatureConfig.getInstance().getProductConfig().getSupportedLanguages().split(","));
        DLog.log("Supported Languages: " + FeatureConfig.getInstance().getProductConfig().getSupportedLanguages());
        if (languages != null && languages.size() > 1) {
            getMenuInflater().inflate(R.menu.menu, menu);
            menu.findItem(R.id.select_polish).setVisible(languages.contains("pl"));
            menu.findItem(R.id.select_french).setVisible(languages.contains("fr"));
            menu.findItem(R.id.select_slovak).setVisible(languages.contains("sk"));
            menu.findItem(R.id.select_spanish).setVisible(languages.contains("es"));
            menu.findItem(R.id.select_portuguese).setVisible(languages.contains("pt")||languages.contains("pt-rBR")||languages.contains("pt-rPT"));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.select_polish) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "pl");
            changeLocale();
        } else if (id == R.id.select_english) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "en");
            changeLocale();
        } else if (id == R.id.select_french) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "fr");
            changeLocale();
        } else if (id == R.id.select_slovak) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "sk");
            changeLocale();
        }else if (id == R.id.select_spanish) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "es");
            changeLocale();
        }else if (id == R.id.select_portuguese) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "pt");
            changeLocale();
        }else if (id == android.R.id.home) {
           onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String storedLocale = PreferenceHelper.getInstance(newBase).getStringItem(Constants.LOCALE);
        if(TextUtils.isEmpty(storedLocale)){
            storedLocale = FeatureConfig.getInstance().getProductConfig().getDefaultLanguage();
        }
        DLog.log("Read locale: " + storedLocale);
        super.attachBaseContext(ContextWrapperHelper.wrap(newBase, storedLocale));
    }

    void changeLocale() {
        Intent intent = new Intent(this, TermsAndConditionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("recreated", true);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    private void killApp() {
        try {
            finish();
//            android.os.Process.killProcess(android.os.Process.myPid());
//            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readMoreTsAndCs(final TextView textView) {
        SpannableString ss = new SpannableString(textView.getText().toString());
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                termsConditionsTv.setText(getResources().getString(R.string.terms_and_conditions_text_more));
                textView.setVisibility(View.GONE);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }


}
