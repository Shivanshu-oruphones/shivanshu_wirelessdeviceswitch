package com.pervacio.wds.custom;

import android.app.ActionBar;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.util.Arrays;
import java.util.List;


/**
 * Created by Surya Polasanapalli on 25-05-2017.
 */

public class TFTermsAndConditionsActivity extends AppCompatActivity {

    PreferenceHelper preferenceHelper;
    private Context mContext;
    private TextView termsConditionsTv;
    Button iAgree, iDisagree, btnExit, btnNext;
    CheckBox iAgreeCheckBox;
    boolean isTelifonicaUK = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        preferenceHelper = PreferenceHelper.getInstance(TFTermsAndConditionsActivity.this);
        getSupportActionBar().setTitle(getString(R.string.terms_and_condition_dialog_header));
        setContentView(R.layout.activity_tf_terms_and_conditions);
        termsConditionsTv = (TextView) findViewById(R.id.terms_conditions_tv);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources()
                    .getColor(R.color.white)));
            TextView textview = new TextView(TFTermsAndConditionsActivity.this);
            RelativeLayout.LayoutParams layoutparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            textview.setLayoutParams(layoutparams);
            textview.setText(getString(R.string.terms_and_condition_dialog_header));
            textview.setGravity(Gravity.CENTER);
            textview.setTextColor(Color.parseColor("#009BDE"));
            textview.setTextSize(25);
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(textview);

            SpannableString spannablePolicyBody;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.permissions_policy_header), Html.FROM_HTML_MODE_COMPACT));
                spannablePolicyBody = new SpannableString(Html.fromHtml(getString(R.string.telifonica_uk_terms_conditions), Html.FROM_HTML_MODE_COMPACT));
            } else {
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.permissions_policy_header)));
                spannablePolicyBody = new SpannableString(Html.fromHtml(getString(R.string.telifonica_uk_terms_conditions)));
            }
            ClickableSpan clickableContent = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent policiIntent = new Intent(getApplicationContext(), TermsAndConditionsActivity.class);
                    policiIntent.putExtra("isContentPolicyScreen", true);
                    startActivity(policiIntent);                }
            };
            ClickableSpan clickablePrivacy = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent policiIntent = new Intent(getApplicationContext(), TermsAndConditionsActivity.class);
                    policiIntent.putExtra("isPrivacyPolicyScreen", true);
                    startActivity(policiIntent);
                }
            };
            String linkText;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                linkText = (new SpannableString(Html.fromHtml(getString(R.string.content_policy), Html.FROM_HTML_MODE_COMPACT))).toString();//getString(R.string.str_terms_condition);
            else
                linkText = (new SpannableString(Html.fromHtml(getString(R.string.content_policy)))).toString();//getString(R.string.str_terms_condition);
            DLog.log("Spanned Text: "+linkText);
            String strPolicyBody = spannablePolicyBody.toString();
            DLog.log("Body Text: "+strPolicyBody);
            spannablePolicyBody.setSpan(clickableContent, strPolicyBody.indexOf(linkText),
                    strPolicyBody.indexOf(linkText) + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_privacy_statement), Html.FROM_HTML_MODE_COMPACT))).toString();//getString(R.string.str_terms_condition);
            else
                linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_privacy_statement)))).toString();//getString(R.string.str_terms_condition);
            spannablePolicyBody.setSpan(clickablePrivacy, strPolicyBody.indexOf(linkText),
                    strPolicyBody.indexOf(linkText) + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            termsConditionsTv.setText(spannablePolicyBody, TextView.BufferType.SPANNABLE);
            termsConditionsTv.setMovementMethod(LinkMovementMethod.getInstance());
            //termsConditionsTv.setText(getString(R.string.telifonica_uk_terms_conditions));
            iAgreeCheckBox = (CheckBox)findViewById(R.id.tc_checkbox);
            btnExit = (Button) findViewById(R.id.exit_btn);
            btnNext = (Button) findViewById(R.id.next_btn);
            iAgreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
                    if(ischecked) {
                        confirmPin();
                        btnNext.setEnabled(true);
                    } else {
                        btnNext.setEnabled(false);
                    }
                }
            });
            btnExit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    killApp();
                }
            });
            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getApplicationContext(), EasyMigrateActivity.class));
                    finish();
                }
            });

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            EMUtility.makeActionOverflowMenuShown(this);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(isTelifonicaUK) {
            menu.clear();
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
        Intent intent = new Intent(this, TFTermsAndConditionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("recreated", true);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    private void killApp() {
        try {
            this.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TFTermsAndConditionsActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("TermsAndConditions", 1);
                editor.commit();
                iAgreeCheckBox.setChecked(true);
                iAgreeCheckBox.setEnabled(false);
                btnNext.setEnabled(true);
                btnNext.setBackgroundResource(R.drawable.round_corners_blue);
                CommonUtil.getInstance().setTermsAndConditionsAccepted(TFTermsAndConditionsActivity.this);
            } else {
                iAgreeCheckBox.setChecked(false);
                iAgreeCheckBox.setEnabled(true);
                btnNext.setEnabled(false);
            }
        }
    }

    private void confirmPin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

            if (km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.terms_and_condition_dialog_header), getString(R.string.confirm_pin));
                startActivityForResult(authIntent, 0);
            } else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TFTermsAndConditionsActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("TermsAndConditions", 1);
                editor.commit();
                iAgreeCheckBox.setChecked(true);
                iAgreeCheckBox.setEnabled(false);
                btnNext.setEnabled(true);
                btnNext.setBackgroundResource(R.drawable.round_corners_blue);
                CommonUtil.getInstance().setTermsAndConditionsAccepted(TFTermsAndConditionsActivity.this);
            }
        }
    }

}
