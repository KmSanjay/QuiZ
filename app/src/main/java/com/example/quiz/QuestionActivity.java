package com.example.quiz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuestionActivity extends AppCompatActivity {
    private static final String FILE_NAME = "QuiZ";
    private static final String KEY_NAME = "Quesition";

    private TextView question,indicator;
    private Button share_btn, next_btn;
    private FloatingActionButton bookmarks_btn;
    private LinearLayout options_container;
    private List<QuestionModel> questionModelsList;
    private int position = 0;
    private int score = 0;
    private int count = 0;
    private String setid;

    // Write a message to the database
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference();
    private Dialog loadingDialog;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private int matchedQuestionPositiion;
    private List<QuestionModel> bookmarkList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        question = findViewById(R.id.quetion_tv);
        indicator = findViewById(R.id.no_indicator_tv);
        bookmarks_btn = findViewById(R.id.bookmarkbtn);
        share_btn = findViewById(R.id.share_btn);
        next_btn = findViewById(R.id.next_btn);
        options_container = findViewById(R.id.options_container);

        preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();

        loadAdds();
        getBookmark();
        bookmarks_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modelMatch()) {
                    bookmarkList.remove(matchedQuestionPositiion);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        bookmarks_btn.setImageDrawable(getDrawable(R.drawable.bookmarks_border));
                    }
                } else {
                    bookmarkList.add(questionModelsList.get(position));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        bookmarks_btn.setImageDrawable(getDrawable(R.drawable.bookmark));
                    }

                }
            }
        });

        setid = getIntent().getStringExtra("setid");

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corner));
        }
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        questionModelsList = new ArrayList<>();
        loadingDialog.show();
        myRef.child("SETS").child(setid)

                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            String id = dataSnapshot.getKey();
                            String question = dataSnapshot.child("question").getValue().toString();
                            String optionA = dataSnapshot.child("optionA").getValue().toString();
                            String optionB = dataSnapshot.child("optionB").getValue().toString();
                            String optionC = dataSnapshot.child("optionC").getValue().toString();
                            String optionD = dataSnapshot.child("optionD").getValue().toString();
                            String answer = dataSnapshot.child("correctAns").getValue().toString();

                            questionModelsList.add(new QuestionModel(id, question, optionA, optionB, optionC, optionD, answer, setid));
                        }
                        if (questionModelsList.size() > 0) {

                            for (int i = 0; i < 4; i++) {
                                options_container.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        checkAnswer(((Button) v));
                                    }
                                });
                            }


                            playAnim(question, 0, questionModelsList.get(position).getQuestion());
                            next_btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    next_btn.setEnabled(false);
                                    next_btn.setAlpha(0.7f);
                                    enableOption(true);
                                    position++;
                                    if (position == questionModelsList.size()) {
                                        Intent scoreIntent = new Intent(QuestionActivity.this, ScoreActivity.class);
                                        scoreIntent.putExtra("score", score);
                                        scoreIntent.putExtra("total", questionModelsList.size());
                                        startActivity(scoreIntent);
                                        finish();
                                        return;

                                    }
                                    count = 0;
                                    playAnim(question, 0, questionModelsList.get(position).getQuestion());
                                }
                            });
                            share_btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String body = questionModelsList.get(position).getQuestion() + "\n" +
                                            questionModelsList.get(position).getA() + "\n" +
                                            questionModelsList.get(position).getB() + "\n" +
                                            questionModelsList.get(position).getC() + "\n" +
                                            questionModelsList.get(position).getD();

                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quiz challenge");
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, body);
                                    startActivity(Intent.createChooser(shareIntent, "share via"));
                                }
                            });

                        } else {
                            finish();
                            Toast.makeText(QuestionActivity.this, "No Questions", Toast.LENGTH_SHORT).show();
                        }
                        loadingDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {
                        Toast.makeText(QuestionActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                        finish();
                    }
                });


    }

    @Override
    protected void onPause() {
        super.onPause();
        storBookmarks();
    }

    private void playAnim(View view, final int value, String data) {

        for (int i = 0; i < 4; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                options_container.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
            }
        }

        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(500).setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (value == 0 && count < 4) {
                    String options = "";
                    if (count == 0) {
                        options = questionModelsList.get(position).getA();
                    } else if (count == 1) {
                        options = questionModelsList.get(position).getB();
                    } else if (count == 2) {
                        options = questionModelsList.get(position).getC();
                    } else if (count == 3) {
                        options = questionModelsList.get(position).getD();
                    }
                    playAnim(options_container.getChildAt(count), 0, options);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    ((TextView) view).setText(data);
                    indicator.setText((position + 1) + "/" + questionModelsList.size());
                    if (modelMatch()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            bookmarks_btn.setImageDrawable(getDrawable(R.drawable.bookmark));
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            bookmarks_btn.setImageDrawable(getDrawable(R.drawable.bookmarks_border));
                        }

                    }
                } catch (ClassCastException ex) {
                    ((Button) view).setText(data);
                }
                view.setTag(data);
                if (value == 0) {
                    playAnim(view, 1, data);
                }else {
                    enableOption(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void checkAnswer(Button selectedOption) {
        enableOption(false);
        next_btn.setEnabled(true);
        next_btn.setAlpha(1);
        if (selectedOption.getText().toString().equals(questionModelsList.get(position).getAnswer())) {
            //correct
            score++;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#55D349")));
            }
        } else {
            //incorrect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff0000")));
            }
            Button correctOptions = options_container.findViewWithTag(questionModelsList.get(position).getAnswer());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                correctOptions.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#55D349")));
            }
        }
    }

    private void enableOption(boolean enable) {
        for (int i = 0; i < 4; i++) {
            options_container.getChildAt(i).setEnabled(enable);
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    options_container.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#848484")));
                }

            }
        }
    }

    private void getBookmark() {
        String json = preferences.getString(KEY_NAME, "");
        Type type = new TypeToken<List<QuestionModel>>() {
        }.getType();

        bookmarkList = gson.fromJson(json, type);
        if (bookmarkList == null) {
            bookmarkList = new ArrayList<>();
        }
    }

    private void storBookmarks() {
        String json = gson.toJson(bookmarkList);
        editor.putString(KEY_NAME, json);
        editor.commit();
    }

    private boolean modelMatch() {
        boolean match = false;
        int i = 0;
        for (QuestionModel questionModel : bookmarkList) {

            if (questionModel.getQuestion().equals(questionModelsList.get(position).getQuestion())
                    && questionModel.getAnswer().equals(questionModelsList.get(position).getAnswer())
                    && questionModel.getSet().equals(questionModelsList.get(position).getSet())) {
                match = true;
                matchedQuestionPositiion = i;
            }
            i++;
        }
        return match;
    }

    private void loadAdds() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

    }

}